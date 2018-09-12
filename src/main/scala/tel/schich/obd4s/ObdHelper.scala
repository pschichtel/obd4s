package tel.schich.obd4s

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.CanFrame
import tel.schich.javacan.isotp.ISOTPAddress._
import tel.schich.javacan.isotp.{FrameHandler, ISOTPBroker, ISOTPChannel}
import tel.schich.obd4s.can.CANObdBridge.{EffPriority, EffTestEquipmentAddress}
import tel.schich.obd4s.obd.{CurrentDataRequests, ModeId, PidSupportReader}
import tel.schich.obd4s.obd.StandardModes.CurrentData

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

object ObdHelper extends StrictLogging {
    def checkResponse(requestSid: Byte, data: Array[Byte]): Result[Array[Byte]] =
        // there must be at least the response code, otherwise this is very weird.
        if (ObdBridge.isMatchingResponse(requestSid, data)) Ok(data)
        else if (ObdBridge.isPositiveResponse(data)) Error(InternalCauses.WrongSid)
        else ObdBridge.getErrorCause(data) match {
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

    def detectECUAddresses(broker: ISOTPBroker, timeout: Duration)(implicit ec: ExecutionContext): Future[Set[Int]] = Future {
        val addresses = mutable.Set[Int]()
        val logger = new AddressLogger(addresses)
        val message = Array[Byte](0x01, 0x00)

        val sffChannel = broker.createChannel(SFF_FUNCTIONAL_ADDRESS, logger)
        val effChannel = broker.createChannel(effAddress(EffPriority, EFF_TYPE_FUNCTIONAL_ADDRESSING, EffTestEquipmentAddress, DESTINATION_EFF_FUNCTIONAL), logger)

        sffChannel.send(message)
        effChannel.send(message)

        Thread.sleep(timeout.toMillis)

        sffChannel.close()
        effChannel.close()

        addresses.toSet.map(returnAddress)
    }

    class AddressLogger(addresses: mutable.Set[Int]) extends FrameHandler {
        override def handleSingleFrame(isotpChannel: ISOTPChannel, i: Int, bytes: Array[Byte]): Unit = addresses.add(i)

        override def handleFirstFrame(isotpChannel: ISOTPChannel, i: Int, bytes: Array[Byte], i1: Int): Unit = addresses.add(i)

        override def handleConsecutiveFrame(isotpChannel: ISOTPChannel, i: Int, bytes: Array[Byte], i1: Int): Unit = addresses.add(i)

        override def handleNonISOTPFrame(canFrame: CanFrame): Unit = {}

        override def checkTimeouts(l: Long): Unit = {}
    }

}
