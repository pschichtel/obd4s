package tel.schich.obd4s.can

import java.nio.ByteBuffer
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}
import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan._
import tel.schich.javacan.IsotpCanChannel.MAX_MESSAGE_LENGTH
import tel.schich.javacan.util.IsotpListener
import tel.schich.javacan.IsotpOptions.Flag
import tel.schich.obd4s._
import tel.schich.obd4s.InternalCause.ResponseTooShort
import tel.schich.obd4s.ObdBridge.{getErrorCause, isMatchingResponse}
import tel.schich.obd4s.obd.{PlainRequest, Reader, ServiceId}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration

object CANObdBridge {
    val EffPriority = 0x18
    val EffTestEquipmentAddress = 0xF1
}

class CANObdBridge(device: NetworkDevice, listener: IsotpListener, ecuAddress: Int, timeout: Duration = Duration(1, SECONDS))(implicit ec: ExecutionContext) extends ObdBridge with StrictLogging {

    private val channel = CanChannels.newIsotpChannel(device, IsotpAddress.returnAddress(ecuAddress), ecuAddress)
    channel.setOption(IsotpCanSocketOptions.OPTS, IsotpOptions.DEFAULT.withFlag(Flag.TX_PADDING))
    listener.addChannel(channel, this.handleResponse)
    private val writeBuffer = ByteBuffer.allocateDirect(MAX_MESSAGE_LENGTH + 1)

    private val requestQueue: mutable.Queue[PendingRequest] = mutable.Queue()

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var inFlightTimeout: ScheduledFuture[_] = _

    override def executeRequest[A](service: ServiceId, reader: Reader[A]): Future[Result[A]] = {
        execAll(service, Nil).map { result =>
            result
                .flatMap(buf => reader.read(buf.view, 0))
                .map(_._1)
        }
    }

    override def executeRequest[A](service: ServiceId, pid: Int, reader: Reader[A]): Future[Result[A]] = {
        execAll(service, Seq(pid)).map { result =>
            result
                .flatMap { buf => readParameter(pid, reader, buf, service.length) }
                .map(_._1)
        }
    }


    override def executeRequest[A, B](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B]): Future[Result[(A, B)]] = {
        execAll(service, Seq(a.pid, b.pid)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readParameter(a, buf, service.length)
                    (br, _ ) <- readParameter(b, buf, oa)
                } yield (ar, br)
            }
        }
    }

    override def executeRequest[A, B, C](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C]): Future[Result[(A, B, C)]] = {
        execAll(service, Seq(a.pid, b.pid, c.pid)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readParameter(a, buf, service.length)
                    (br, ob) <- readParameter(b, buf, oa)
                    (cr, _ ) <- readParameter(c, buf, ob)
                } yield (ar, br, cr)
            }
        }
    }

    override def executeRequest[A, B, C, D](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D]): Future[Result[(A, B, C, D)]] = {
        execAll(service, Seq(a.pid, b.pid, c.pid, d.pid)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readParameter(a, buf, service.length)
                    (br, ob) <- readParameter(b, buf, oa)
                    (cr, oc) <- readParameter(c, buf, ob)
                    (dr, _ ) <- readParameter(d, buf, oc)
                } yield (ar, br, cr, dr)
            }
        }
    }

    override def executeRequest[A, B, C, D, E](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E]): Future[Result[(A, B, C, D, E)]] = {
        execAll(service, Seq(a.pid, b.pid, c.pid, d.pid, e.pid)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readParameter(a, buf, service.length)
                    (br, ob) <- readParameter(b, buf, oa)
                    (cr, oc) <- readParameter(c, buf, ob)
                    (dr, od) <- readParameter(d, buf, oc)
                    (er, _ ) <- readParameter(e, buf, od)
                } yield (ar, br, cr, dr, er)
            }
        }
    }

    override def executeRequest[A, B, C, D, E, F](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E], f: PlainRequest[F]): Future[Result[(A, B, C, D, E, F)]] = {
        execAll(service, Seq(a.pid, b.pid, c.pid, d.pid, e.pid, f.pid)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readParameter(a, buf, service.length)
                    (br, ob) <- readParameter(b, buf, oa)
                    (cr, oc) <- readParameter(c, buf, ob)
                    (dr, od) <- readParameter(d, buf, oc)
                    (er, oe) <- readParameter(e, buf, od)
                    (fr, _ ) <- readParameter(f, buf, oe)
                } yield (ar, br, cr, dr, er, fr)
            }
        }
    }

    override def executeRequests[A](service: ServiceId, reqs: Seq[PlainRequest[_ <: A]]): Future[Result[Seq[_ <: A]]] = {

        @tailrec
        def parseResponse(buf: Array[Byte], offset: Int, reqs: Seq[PlainRequest[_ <: A]], result: Result[Seq[_ <: A]]): Result[Seq[_ <: A]] = {
            if (reqs.isEmpty) result
            else if (offset > buf.length) Error(ResponseTooShort)
            else {
                result match {
                    case Ok(responses) =>
                        val r = reqs.head
                        readParameter(r, buf, offset) match {
                            case Ok((response, byteRead)) =>
                                parseResponse(buf, offset + byteRead, reqs.tail, Ok(responses :+ response))
                            case Error(reason) => Error(reason)
                        }
                    case e @ Error(_) => e
                }
            }
        }

        execAll(service, reqs.map(_.pid)).map { result =>
            result.flatMap { buf => parseResponse(buf, service.length, reqs, Ok(Seq.empty)) }
        }
    }

    private def readParameter[A](req: PlainRequest[A], buf: Array[Byte], offset: Int): Result[(A, Int)] = {
        readParameter(req.pid, req.reader, buf, offset)
    }

    private def readParameter[A](pid: Int, reader: Reader[A], buf: Array[Byte], offset: Int): Result[(A, Int)] = {
        if (offset >= buf.length) Error(InternalCause.ResponseTooShort)
        else if ((buf(offset) & 0xFF) != pid) Error(PidMismatch(pid, offset, buf))
        else reader.read(buf.view, offset + 1) map {
            case (result, bytesRead) => (result, bytesRead + 1)
        }
    }

    private def execAll(mode: ServiceId, pids: Seq[Int]): Future[Result[Array[Byte]]] = {

        val sidByte = (mode.id & 0xFF).toByte
        val msg = (mode.id +: pids).map(p => (p & 0xFF).toByte).toArray

        val promise = Promise[Result[Array[Byte]]]()
        enqueue(PendingRequest(sidByte, msg, promise))

        promise.future
    }

    private def enqueue(req: PendingRequest): Unit = synchronized {
        val wasEmpty = requestQueue.isEmpty
        requestQueue.enqueue(req)
        if (wasEmpty) {
            sendNext()
        }
    }

    private def cancelTimeout(): Unit = synchronized {
        if (inFlightTimeout != null) {
            // safe to cancel even if already done, but don't interrupt as this might get called from the task itself
            inFlightTimeout.cancel(false)
            inFlightTimeout = null
        }
    }

    private def sendNext(): Unit = synchronized {
        if (requestQueue.nonEmpty) {
            val next = requestQueue.head.msg
            writeBuffer.clear()
            writeBuffer.put(next)
            writeBuffer.flip()
            channel.write(writeBuffer)
            cancelTimeout()
            val timeoutTask = new Runnable {
                override def run(): Unit = timeoutInflightRequest()
            }
            inFlightTimeout = scheduledExecutorService.schedule(timeoutTask, timeout.toMillis, MILLISECONDS)
        }
    }

    private def consume(): PendingRequest = synchronized {
        val entry = requestQueue.dequeue()
        cancelTimeout()
        sendNext()
        entry
    }

    private def handleResponse(ch: IsotpCanChannel, message: ByteBuffer): Unit = synchronized {
        // don't handle messages, when we don't have an in-flight request
        if (requestQueue.nonEmpty) {
            val req = requestQueue.head
            if (isMatchingResponse(req.sid, message)) {
                consume()
                req.promise.success(Ok(ObdHelper.getMessage(message)))
            } else getErrorCause(message) match {
                case Some(cause) =>
                    consume()
                    // yep, it's an error
                    req.promise.success(Error(cause))
                case _ =>
                // don't know what it is, but also don't care
            }
        }
    }

    private def timeoutInflightRequest(): Unit = synchronized {
        consume().promise.success(Error(InternalCause.Timeout))
    }

    private case class PendingRequest(sid: Byte, msg: Array[Byte], promise: Promise[Result[Array[Byte]]])
}
