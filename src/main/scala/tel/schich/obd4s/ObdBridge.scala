package tel.schich.obd4s

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.CanFrame.MAX_DATA_LENGTH
import tel.schich.obd4s.obd._

import scala.concurrent.Future

object ObdBridge {
    val SupportRangeSize: Int = 0x20
    val MaximumPid: Int = 255
    val PositiveResponseBase: Byte = 0x40
    val NegativeResponseCode: Byte = 0x7F.toByte
    val MaxRequestPids: Int = MAX_DATA_LENGTH - 2 // 2 = Single-Frame PCI Size + SID Size

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

    def executeRequest[M <: Mode, A](a: Request[A, M]): Future[Result[A]] =
        executeRequest(a.mode.id, a.pid, a.reader)
    def executeRequest[M <: Mode, A, B](a: Request[A, M], b: Request[B, M]): Future[Result[(A, B)]] =
        executeRequest(a.mode.id, a.plain, b.plain)
    def executeRequest[M <: Mode, A, B, C](a: Request[A, M], b: Request[B, M], c: Request[C, M]): Future[Result[(A, B, C)]] =
        executeRequest(a.mode.id, a.plain, b.plain, c.plain)
    def executeRequest[M <: Mode, A, B, C, D](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M]): Future[Result[(A, B, C, D)]] =
        executeRequest(a.mode.id, a.plain, b.plain, c.plain, d.plain)
    def executeRequest[M <: Mode, A, B, C, D, E](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M], e: Request[E, M]): Future[Result[(A, B, C, D, E)]] =
        executeRequest(a.mode.id, a.plain, b.plain, c.plain, d.plain, e.plain)
    def executeReuqest[M <: Mode, A, B, C, D, E, F](a: Request[A, M], b: Request[B, M], c: Request[C, M], d: Request[D, M], e: Request[E, M], f: Request[F, M]): Future[Result[(A, B, C, D, E, F)]] =
        executeRequest(a.mode.id, a.plain, b.plain, c.plain, d.plain, e.plain, f.plain)
    def executeRequests[M <: Mode, A](reqs: Seq[Request[_ <: A, M]]): Future[Result[Seq[_ <: A]]] =
        if (reqs.isEmpty) Future.failed(new IllegalArgumentException("No requests given!"))
        else executeRequests[A](reqs.head.mode.id, reqs.map(_.plain))

    def executeRequest(mode: ModeId): Future[Unit]
    def executeRequest[A](mode: ModeId, pid: Int, reader: Reader[A]): Future[Result[A]]
    def executeRequest[A](mode: ModeId, a: PlainRequest[A]): Future[Result[A]] = executeRequest(mode, a.pid, a.reader)
    def executeRequest[A, B](mode: ModeId, a: PlainRequest[A], b: PlainRequest[B]): Future[Result[(A, B)]]
    def executeRequest[A, B, C](mode: ModeId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C]): Future[Result[(A, B, C)]]
    def executeRequest[A, B, C, D](mode: ModeId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D]): Future[Result[(A, B, C, D)]]
    def executeRequest[A, B, C, D, E](mode: ModeId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E]): Future[Result[(A, B, C, D, E)]]
    def executeRequest[A, B, C, D, E, F](mode: ModeId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E], f: PlainRequest[F]): Future[Result[(A, B, C, D, E, F)]]
    def executeRequests[A](mode: ModeId, reqs: Seq[PlainRequest[_ <: A]]): Future[Result[Seq[_ <: A]]]
}
