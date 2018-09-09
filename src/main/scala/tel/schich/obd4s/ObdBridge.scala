package tel.schich.obd4s

import com.typesafe.scalalogging.StrictLogging
import tel.schich.obd4s.obd._

import scala.concurrent.Future

object ObdBridge {
    val SupportRangeSize: Int = 0x20
    val MaximumPid: Int = 255
    val PositiveResponseBase: Byte = 0x40
    val NegativeResponseCode: Byte = 0x7F.toByte

    def isMatchingResponse(requestSid: Byte, data: Array[Byte]): Boolean =
        isPositiveResponse(data) && (requestSid + PositiveResponseBase) == data(0)

    def isPositiveResponse(data: Array[Byte]): Boolean =
        data.nonEmpty && data(0) >= PositiveResponseBase

    def isErrorResponse(data: Array[Byte]): Boolean =
        data.nonEmpty && data(0) == NegativeResponseCode

    def getErrorCause(data: Array[Byte]): Option[Cause] =
        if (!ObdBridge.isErrorResponse(data) || data.length < 2) None
        else Some(ObdCauses.lookupByCode.getOrElse(data(1), InternalCauses.UnknownCause))
}

trait ObdBridge extends StrictLogging {

    type Req[T] = (Int, Reader[T])

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
    def executeRequest[M <: Mode, A](reqs: Seq[Request[A, M]]): Future[Result[Seq[A]]] =
        if (reqs.isEmpty) Future.failed(new IllegalArgumentException("No requests given!"))
        else executeRequest(reqs.head.mode.id, reqs.map(_.tupled))

    def executeRequest(mode: ModeId): Future[Unit]
    def executeRequest[A](mode: ModeId, pid: Int, reader: Reader[A]): Future[Result[A]]
    def executeRequest[A](mode: ModeId, a: Req[A]): Future[Result[A]] = executeRequest(mode, a._1, a._2)
    def executeRequest[A, B](mode: ModeId, a: Req[A], b: Req[B]): Future[Result[(A, B)]]
    def executeRequest[A, B, C](mode: ModeId, a: Req[A], b: Req[B], c: Req[C]): Future[Result[(A, B, C)]]
    def executeRequest[A, B, C, D](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D]): Future[Result[(A, B, C, D)]]
    def executeRequest[A, B, C, D, E](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E]): Future[Result[(A, B, C, D, E)]]
    def executeRequest[A, B, C, D, E, F](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E], f: Req[F]): Future[Result[(A, B, C, D, E, F)]]
    def executeRequest[A](mode: ModeId, reqs: Seq[Req[A]]): Future[Result[Seq[A]]]
}
