package tel.schich.obd4s

import com.typesafe.scalalogging.StrictLogging
import tel.schich.obd4s.obd._
import tel.schich.obd4s.obd.StandardModes.CurrentData

import scala.concurrent.{ExecutionContext, Future}

object ObdBridge {
    val SupportRangeSize: Int = 0x20
    val MaximumPid: Int = 255
    val SuccessfulResponseBase: Byte = 0x40
}

trait ObdBridge extends StrictLogging {

    type Req[T] = (Int, Reader[T])

    def detectSupport()(implicit ec: ExecutionContext): Future[Int => Boolean] = {

        def scanSupport(pid: Int, currentSet: Vector[Boolean]): Future[Vector[Boolean]] = {
            if (currentSet.nonEmpty && !currentSet.last) Future.successful(currentSet)
            else if (pid > ObdBridge.MaximumPid) Future.successful(currentSet)
            else executeRequest[BitSet](CurrentData.id, pid, PidSupportReader) flatMap {
                case Ok(bitSet) => scanSupport(pid + ObdBridge.SupportRangeSize, currentSet ++ bitSet.set)
                case Error(reason) =>
                    logger.error(s"Support detection failed: $reason")
                    Future.successful(currentSet)
            }
        }

        scanSupport(ObdRequests.Support01To20.pid, Vector(true)) map { supportVector =>
            val support = supportVector.applyOrElse(_: Int, (_: Int) => false)
            ObdRequests.values.foreach {
                case r if r.isSupported(support) =>
                    logger.info(s"Supported: ${r.name}")
                case r =>
                    logger.info(s"Unsupported: ${r.name} (PID=${r.pid})")
            }
            support
        }
    }

    def executeRequest[M <: Mode, A](a: Request[A, M]): Future[Result[A]] =
        executeRequest(a.mode.id, a.pid, a.reader)
    def executeRequest[M <: Mode, A, B](a: Request[A, M], b: Request[B, M]): Future[Result[(A, B)]] =
        executeRequest(a.mode.id, a.tupled, b.tupled)
    def executeRequest[M <: Mode, A, B, C](a: Request[A, M], b: Request[B, M], c: Request[C, M]): Future[Result[(A, B, C)]] =
        executeRequest(a.mode.id, a.tupled, b.tupled, c.tupled)
    def executeRequest[M <: Mode, A, B, C, D](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M]): Future[Result[(A, B, C, D)]] =
        executeRequest(a.mode.id, a.tupled, b.tupled, c.tupled, d.tupled)
    def executeRequest[M <: Mode, A, B, C, D, E](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M], e: Request[E, M]): Future[Result[(A, B, C, D, E)]] =
        executeRequest(a.mode.id, a.tupled, b.tupled, c.tupled, d.tupled, e.tupled)
    def executeReuqest[M <: Mode, A, B, C, D, E, F](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M], e: Request[E, M], f: Request[F, M]): Future[Result[(A, B, C, D, E, F)]] =
        executeRequest(a.mode.id, a.tupled, b.tupled, c.tupled, d.tupled, e.tupled, f.tupled)

    def executeRequest[A](mode: ModeId, pid: Int, reader: Reader[A]): Future[Result[A]]
    def executeRequest[A](mode: ModeId, a: Req[A]): Future[Result[A]] = executeRequest(mode, a._1, a._2)
    def executeRequest[A, B](mode: ModeId, a: Req[A], b: Req[B]): Future[Result[(A, B)]]
    def executeRequest[A, B, C](mode: ModeId, a: Req[A], b: Req[B], c: Req[C]): Future[Result[(A, B, C)]]
    def executeRequest[A, B, C, D](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D]): Future[Result[(A, B, C, D)]]
    def executeRequest[A, B, C, D, E](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E]): Future[Result[(A, B, C, D, E)]]
    def executeRequest[A, B, C, D, E, F](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E], f: Req[F]): Future[Result[(A, B, C, D, E, F)]]
}
