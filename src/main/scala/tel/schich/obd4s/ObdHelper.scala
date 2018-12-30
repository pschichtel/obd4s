package tel.schich.obd4s

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey.OP_READ

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.CanFrame.FD_NO_FLAGS
import tel.schich.javacan.CanSocketOptions.FILTER
import tel.schich.javacan.IsotpAddress._
import tel.schich.javacan._
import tel.schich.obd4s.can.CANObdBridge.{EffPriority, EffTestEquipmentAddress}
import tel.schich.obd4s.obd.CurrentDataRequests.Support01To20
import tel.schich.obd4s.obd.StandardModes.CurrentData
import tel.schich.obd4s.obd.{CurrentDataRequests, ModeId, PidSupportReader}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

object ObdHelper extends StrictLogging {
    val EffFunctionalFilter = new CanFilter(effAddress(EffPriority, EFF_TYPE_FUNCTIONAL_ADDRESSING, 0, EffTestEquipmentAddress), EFF_MASK_FUNCTIONAL_RESPONSE)

    val EffFunctionalAddress: Int = effAddress(EffPriority, EFF_TYPE_FUNCTIONAL_ADDRESSING, EffTestEquipmentAddress, DESTINATION_EFF_FUNCTIONAL)
    val EcuDetectionMessage: Array[Byte] = 0x02.toByte +: (CurrentData.id.bytes ++ Support01To20.bytes)
    val SffEcuDetectionFrame: CanFrame = CanFrame.create(SFF_FUNCTIONAL_ADDRESS, FD_NO_FLAGS, EcuDetectionMessage)
    val EffEcuDetectionFrame: CanFrame = CanFrame.create(EffFunctionalAddress, FD_NO_FLAGS, EcuDetectionMessage)

    def hexDump(bytes: Seq[Byte]): String = {
        bytes.map(b => (b & 0xFF).toHexString.toUpperCase.reverse.padTo(2, '0').reverse).mkString(".")
    }

    def hexDump(bytes: ByteBuffer, offset: Int, length: Int): String = {
        (0 until length)
            .map(i => bytes.get(offset + i))
            .map(b => (b & 0xFF).toHexString.toUpperCase.reverse.padTo(2, '0').reverse)
            .mkString(".")
    }

    def addressAsHex(addr: Int): String = {
        val padLen = if (CanId.isExtended(addr)) 29/4+1 else 11/4+1
        addr.toHexString.toUpperCase.reverse.padTo(padLen, '0').reverse
    }

    def asHex(num: Long): String = {
        val hex = num.toHexString
        if ((hex.length & 1) == 0) hex
        else s"0$hex"
    }

    def getMessage(data: ByteBuffer, offset: Int, length: Int): Array[Byte] = {
        val out = Array.ofDim[Byte](length)
        data.position(offset)
        data.get(out)
        out
    }

    def checkResponse(requestSid: Byte, data: ByteBuffer, offset: Int, length: Int): Result[Array[Byte]] =
        // there must be at least the response code, otherwise this is very weird.
        if (ObdBridge.isMatchingResponse(requestSid, data, offset, length)) Ok(getMessage(data, offset, length))
        else if (ObdBridge.isPositiveResponse(data, offset, length)) Error(InternalCauses.WrongSid)
        else ObdBridge.getErrorCause(data, offset, length) match {
            case Some(cause) => Error(cause)
            case _ => Error(InternalCauses.UnknownResponse)
        }



    def detectSupport(bridge: ObdBridge, service: ModeId = CurrentData.id)(implicit ec: ExecutionContext): Future[Result[Int => Boolean]] = {

        def scanSupport(pid: Int, currentSet: Vector[Boolean]): Future[Result[Vector[Boolean]]] = {
            if (currentSet.nonEmpty && !currentSet.last) Future.successful(Ok(currentSet))
            else if (pid > ObdBridge.MaximumPid) Future.successful(Ok(currentSet))
            else bridge.executeRequest(service, pid, PidSupportReader) flatMap {
                case Ok(bitSet) =>
                    logger.trace(s"Support detected with ${pid.toHexString}: ${bitSet.set.mkString(",")}")
                    scanSupport(pid + ObdBridge.SupportRangeSize, currentSet ++ bitSet.set)
                case Error(cause) => Future.successful(Error(cause))
            }
        }

        scanSupport(CurrentDataRequests.Support01To20.pid, Vector(true)) map { result =>
            result.map(support => support.applyOrElse(_: Int, (_: Int) => false))
        }
    }

    def detectECUAddresses(device: CanDevice, timeout: Duration): Set[Int] = {
        val addresses = mutable.Set[Int]()

        val ch = CanChannels.newRawChannel(device)

        ch.setOption(FILTER, Array(IsotpAddress.SFF_FUNCTIONAL_FILTER, EffFunctionalFilter))
        ch.configureBlocking(false)

        val selector = ch.provider().openSelector()
        ch.register(selector, OP_READ)

        ch.write(SffEcuDetectionFrame)
        ch.write(EffEcuDetectionFrame)

        val startTime = System.currentTimeMillis()
        while ((System.currentTimeMillis() - startTime) <= timeout.toMillis) {
            val n = selector.select(timeout.toMillis)
            if (n > 0) {
                selector.selectedKeys().clear()
                val frame = ch.read()
                addresses += frame.getId
            }
        }

        selector.close()
        ch.close()

        addresses.toSet.map(returnAddress)
    }

}
