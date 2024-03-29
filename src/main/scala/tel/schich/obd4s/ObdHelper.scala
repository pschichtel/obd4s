package tel.schich.obd4s

import java.nio.ByteBuffer
import java.nio.channels.spi.SelectorProvider
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadFactory
import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.util.CanBroker
import tel.schich.javacan.{CanFilter, CanFrame, CanId, IsotpAddress, NetworkDevice}
import tel.schich.javacan.platform.linux.UnixFileDescriptor
import tel.schich.javacan.select.IOSelector
import tel.schich.javacan.IsotpAddress.{effAddress, returnAddress}
import tel.schich.obd4s.can.CANObdBridge.{EffPriority, EffTestEquipmentAddress}
import tel.schich.obd4s.elm.ElmCommands.CANReceiveFilter
import tel.schich.obd4s.obd.CurrentDataRequest.Support01To20
import tel.schich.obd4s.obd.StandardMode.CurrentData
import tel.schich.obd4s.obd.{CurrentDataRequest, PidSupportReader, ServiceId}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{blocking, ExecutionContext, Future}

object ObdHelper extends StrictLogging {
    val EffFunctionalFilter = new CanFilter(effAddress(EffPriority, IsotpAddress.EFF_TYPE_FUNCTIONAL_ADDRESSING, 0, EffTestEquipmentAddress), IsotpAddress.EFF_MASK_FUNCTIONAL_RESPONSE)

    val EffFunctionalAddress: Int = effAddress(EffPriority, IsotpAddress.EFF_TYPE_FUNCTIONAL_ADDRESSING, EffTestEquipmentAddress, IsotpAddress.DESTINATION_EFF_FUNCTIONAL)
    val EcuDetectionMessage: Array[Byte] = isotpSingleFrame(CurrentData.id.bytes ++ Support01To20.bytes)
    val SffEcuDetectionFrame: CanFrame = CanFrame.create(IsotpAddress.SFF_FUNCTIONAL_ADDRESS, CanFrame.FD_NO_FLAGS, EcuDetectionMessage)
    val EffEcuDetectionFrame: CanFrame = CanFrame.createExtended(EffFunctionalAddress, CanFrame.FD_NO_FLAGS, EcuDetectionMessage)

    def hexDump(bytes: Iterable[Byte]): String = {
        bytes.map(b => (b & 0xFF).toHexString.toUpperCase.reverse.padTo(2, '0').reverse).mkString(".")
    }

    def isotpSingleFrame(payload: Array[Byte]): Array[Byte] = {
        val maxBytes = CanFrame.MAX_DATA_LENGTH - 1
        if (payload.length >= maxBytes) {
            throw new IllegalArgumentException(s"Payload may only be $maxBytes bytes long")
        }
        val frame = Array.fill[Byte](CanFrame.MAX_DATA_LENGTH)(0)
        frame(0) = payload.length.toByte
        payload.copyToArray(frame, 1)
        frame
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

    def getMessage(data: ByteBuffer): Array[Byte] = {
        val out = Array.ofDim[Byte](data.remaining())
        data.get(out)
        out
    }

    def checkResponse(requestSid: Byte, data: ByteBuffer): Result[Array[Byte]] =
        // there must be at least the response code, otherwise this is very weird.
        if (ObdBridge.isMatchingResponse(requestSid, data)) Ok(getMessage(data))
        else if (ObdBridge.isPositiveResponse(data)) Error(InternalCause.WrongSid)
        else ObdBridge.getErrorCause(data) match {
            case Some(cause) => Error(cause)
            case _ => Error(InternalCause.UnknownResponse)
        }



    def detectSupport(bridge: ObdBridge, service: ServiceId = CurrentData.id)(implicit ec: ExecutionContext): Future[Result[Vector[Boolean]]] = {

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

        scanSupport(CurrentDataRequest.Support01To20.pid, Vector(true))
    }

    def detectECUAddresses(device: NetworkDevice, threadFactory: ThreadFactory, provider: IOSelector[UnixFileDescriptor], timeout: Duration)(implicit ec: ExecutionContext): Future[Set[Int]] = Future {
        val addresses = mutable.Set[Int]()
        val broker = new CanBroker(threadFactory, provider, java.time.Duration.of(timeout.toMillis, ChronoUnit.MILLIS))
        broker.addFilter(IsotpAddress.SFF_FUNCTIONAL_FILTER)
        broker.addFilter(EffFunctionalFilter)

        broker.addDevice(device, (_, frame) => {
            addresses += frame.getId
        })

        broker.send(SffEcuDetectionFrame)
        broker.send(EffEcuDetectionFrame)
        try {
            blocking {
                Thread.sleep(timeout.toMillis)
            }
        } finally {
            broker.close()
        }

        addresses.toSet.map(returnAddress)
    }
}
