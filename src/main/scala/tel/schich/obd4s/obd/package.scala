package tel.schich.obd4s

import tel.schich.obd4s.obd.StandardModes.CurrentData

import scala.concurrent.{ExecutionContext, Future}

package object obd {


    def detectSupport(bridge: ObdBridge, service: ModeId = CurrentData.id)(implicit ec: ExecutionContext): Future[Int => Boolean] = {

        def scanSupport(pid: Int, currentSet: Vector[Boolean]): Future[Vector[Boolean]] = {
            if (currentSet.nonEmpty && !currentSet.last) Future.successful(currentSet)
            else if (pid > ObdBridge.MaximumPid) Future.successful(currentSet)
            else bridge.executeRequest[BitSet](service, pid, PidSupportReader) flatMap {
                case Ok(bitSet) => scanSupport(pid + ObdBridge.SupportRangeSize, currentSet ++ bitSet.set)
                case Error(cause) => Future.failed(new ObdException(cause))
            }
        }

        scanSupport(CurrentDataRequests.Support01To20.pid, Vector(true)) map { supportVector =>
            supportVector.applyOrElse(_: Int, (_: Int) => false)
        }
    }
}
