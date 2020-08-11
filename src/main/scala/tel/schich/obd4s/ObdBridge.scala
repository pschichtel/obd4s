package tel.schich.obd4s

import java.nio.ByteBuffer

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.CanFrame.MAX_DATA_LENGTH
import tel.schich.obd4s.obd.ObdCauses.NegativeResponseCode
import tel.schich.obd4s.obd._

import scala.concurrent.Future

object ObdBridge {
    val SupportRangeSize: Int = 0x20
    val MaximumPid: Int = 255
    val PositiveResponseBase: Byte = 0x40
    val MaxRequestPids: Int = MAX_DATA_LENGTH - 2 // 2 = Single-Frame PCI Size + SID Size

    def isMatchingResponse(requestSid: Byte, data: ByteBuffer): Boolean =
        isPositiveResponse(data) && (requestSid + PositiveResponseBase) == data.get(data.position())

    def isPositiveResponse(data: ByteBuffer): Boolean =
        data.hasRemaining && data.get(data.position()) >= PositiveResponseBase

    def isErrorResponse(data: ByteBuffer): Boolean =
        data.hasRemaining && data.get(data.position()) == NegativeResponseCode

    def getErrorCause(data: ByteBuffer): Option[Cause] =
        if (!ObdBridge.isErrorResponse(data) || data.remaining() < 2) None
        else Some(ObdCauses.lookupByCode.getOrElse(data.get(data.position() + 1), InternalCauses.UnknownCause))
}

trait ObdBridge extends StrictLogging {

    def requestService[S <: Service, A](req: ServiceRequest[A, S]): Future[Result[A]] =
        executeRequest(req.service.id, req.reader)

    def requestParameter[S <: Service, A](a: ParameterRequest[A, S]): Future[Result[A]] =
        executeRequest(a.service.id, a.pid, a.reader)
    def requestParameters[S <: Service, A, B](a: ParameterRequest[A, S], b: ParameterRequest[B, S]): Future[Result[(A, B)]] =
        executeRequest(a.service.id, a.plain, b.plain)
    def requestParameters[S <: Service, A, B, C](a: ParameterRequest[A, S], b: ParameterRequest[B, S], c: ParameterRequest[C, S]): Future[Result[(A, B, C)]] =
        executeRequest(a.service.id, a.plain, b.plain, c.plain)
    def requestParameters[S <: Service, A, B, C, D](a: ParameterRequest[A, S], b: ParameterRequest[B, S], c: ParameterRequest[C, S], d: ParameterRequest[D, S]): Future[Result[(A, B, C, D)]] =
        executeRequest(a.service.id, a.plain, b.plain, c.plain, d.plain)
    def requestParameters[S <: Service, A, B, C, D, E](a: ParameterRequest[A, S], b: ParameterRequest[B, S], c: ParameterRequest[C, S], d: ParameterRequest[D, S], e: ParameterRequest[E, S]): Future[Result[(A, B, C, D, E)]] =
        executeRequest(a.service.id, a.plain, b.plain, c.plain, d.plain, e.plain)
    def requestParameters[S <: Service, A, B, C, D, E, F](a: ParameterRequest[A, S], b: ParameterRequest[B, S], c: ParameterRequest[C, S], d: ParameterRequest[D, S], e: ParameterRequest[E, S], f: ParameterRequest[F, S]): Future[Result[(A, B, C, D, E, F)]] =
        executeRequest(a.service.id, a.plain, b.plain, c.plain, d.plain, e.plain, f.plain)
    def requestParameters[S <: Service, A](reqs: Seq[ParameterRequest[_ <: A, S]]): Future[Result[Seq[_ <: A]]] =
        if (reqs.isEmpty) Future.failed(new IllegalArgumentException("No requests given!"))
        else executeRequests[A](reqs.head.service.id, reqs.map(_.plain))

    def executeRequest[A](service: ServiceId, reader: Reader[A]): Future[Result[A]]
    def executeRequest[A](service: ServiceId, pid: Int, reader: Reader[A]): Future[Result[A]]
    def executeRequest[A](service: ServiceId, a: PlainRequest[A]): Future[Result[A]] = executeRequest(service, a.pid, a.reader)
    def executeRequest[A, B](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B]): Future[Result[(A, B)]]
    def executeRequest[A, B, C](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C]): Future[Result[(A, B, C)]]
    def executeRequest[A, B, C, D](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D]): Future[Result[(A, B, C, D)]]
    def executeRequest[A, B, C, D, E](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E]): Future[Result[(A, B, C, D, E)]]
    def executeRequest[A, B, C, D, E, F](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E], f: PlainRequest[F]): Future[Result[(A, B, C, D, E, F)]]
    def executeRequests[A](service: ServiceId, reqs: Seq[PlainRequest[_ <: A]]): Future[Result[Seq[_ <: A]]]
}
