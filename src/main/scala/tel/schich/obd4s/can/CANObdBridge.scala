package tel.schich.obd4s.can

import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.isotp.AggregatingFrameHandler.aggregateFrames
import tel.schich.javacan.isotp.{ISOTPBroker, ISOTPChannel}
import tel.schich.obd4s.InternalCauses.ResponseTooShort
import tel.schich.obd4s.ObdBridge.{getErrorCause, isMatchingResponse}
import tel.schich.obd4s._
import tel.schich.obd4s.obd.{ModeId, Reader}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}

object CANObdBridge {
    val EffPriority = 0x18
    val EffTestEquipmentAddress = 0xF1
}

class CANObdBridge(broker: ISOTPBroker, ecuAddress: Int, timeout: Duration = Duration(1, SECONDS))(implicit ec: ExecutionContext) extends ObdBridge with StrictLogging {

    private val channel = broker.createChannel(ecuAddress, aggregateFrames(this.handleResponse, this.handleIsotpTimeout))

    private val requestQueue: mutable.Queue[PendingRequest] = mutable.Queue()

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var inFlightTimeout: ScheduledFuture[_] = _

    override def executeRequest(mode: ModeId): Future[Unit] = {
        execAll(mode, Nil).map(_ => ())
    }

    override def executeRequest[A](mode: ModeId, pid: Int, reader: Reader[A]): Future[Result[A]] = {
        execAll(mode, Seq(pid)).map { result =>
            result
                .flatMap { buf => readPid(pid, reader, buf, mode.length) }
                .map(_._1)
        }
    }


    override def executeRequest[A, B](mode: ModeId, a: Req[A], b: Req[B]): Future[Result[(A, B)]] = {
        execAll(mode, Seq(a._1, b._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readPid(a, buf, mode.length)
                    (br, _ ) <- readPid(b, buf, oa)
                } yield (ar, br)
            }
        }
    }

    override def executeRequest[A, B, C](mode: ModeId, a: Req[A], b: Req[B], c: Req[C]): Future[Result[(A, B, C)]] = {
        execAll(mode, Seq(a._1, b._1, c._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readPid(a, buf, mode.length)
                    (br, ob) <- readPid(b, buf, oa)
                    (cr, _ ) <- readPid(c, buf, ob)
                } yield (ar, br, cr)
            }
        }
    }

    override def executeRequest[A, B, C, D](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D]): Future[Result[(A, B, C, D)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readPid(a, buf, mode.length)
                    (br, ob) <- readPid(b, buf, oa)
                    (cr, oc) <- readPid(c, buf, ob)
                    (dr, _ ) <- readPid(d, buf, oc)
                } yield (ar, br, cr, dr)
            }
        }
    }

    override def executeRequest[A, B, C, D, E](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E]): Future[Result[(A, B, C, D, E)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1, e._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readPid(a, buf, mode.length)
                    (br, ob) <- readPid(b, buf, oa)
                    (cr, oc) <- readPid(c, buf, ob)
                    (dr, od) <- readPid(d, buf, oc)
                    (er, _ ) <- readPid(e, buf, od)
                } yield (ar, br, cr, dr, er)
            }
        }
    }

    override def executeRequest[A, B, C, D, E, F](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E], f: Req[F]): Future[Result[(A, B, C, D, E, F)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1, e._1, f._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- readPid(a, buf, mode.length)
                    (br, ob) <- readPid(b, buf, oa)
                    (cr, oc) <- readPid(c, buf, ob)
                    (dr, od) <- readPid(d, buf, oc)
                    (er, oe) <- readPid(e, buf, od)
                    (fr, _ ) <- readPid(f, buf, oe)
                } yield (ar, br, cr, dr, er, fr)
            }
        }
    }

    override def executeRequest[A](mode: ModeId, reqs: Seq[Req[A]]): Future[Result[Seq[A]]] = {

        @tailrec
        def parseResponse(buf: Array[Byte], offset: Int, reqs: Seq[Req[A]], result: Result[Seq[A]]): Result[Seq[A]] = {
            if (reqs.isEmpty) result
            else if (offset > buf.length) Error(ResponseTooShort)
            else {
                result match {
                    case Ok(responses) =>
                        val r = reqs.head
                        readPid(r, buf, offset) match {
                            case Ok((response, byteRead)) =>
                                parseResponse(buf, offset + byteRead, reqs.tail, Ok(responses :+ response))
                            case Error(reason) => Error(reason)
                        }
                    case e @ Error(_) => e
                }
            }
        }

        execAll(mode, reqs.map(_._1)).map { result =>
            result.flatMap { buf => parseResponse(buf, mode.length, reqs, Ok(Seq.empty)) }
        }
    }

    private def readPid[A](req: Req[A], buf: IndexedSeq[Byte], offset: Int): Result[(A, Int)] = {
        readPid(req._1, req._2, buf, offset)
    }

    private def readPid[A](pid: Int, reader: Reader[A], buf: IndexedSeq[Byte], offset: Int): Result[(A, Int)] = {
        if (offset >= buf.length) Error(InternalCauses.ResponseTooShort)
        else if (buf(offset) != pid) Error(InternalCauses.PidMismatch)
        else reader.read(buf, offset + 1)
    }

    private def execAll(mode: ModeId, pids: Seq[Int]): Future[Result[Array[Byte]]] = {

        val sidByte = (mode.id & 0xFF).toByte
        val msg = (mode.id +: pids).map(p => (p & 0xFF).toByte).toArray

        val promise = Promise[Result[Array[Byte]]]
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
            channel.send(requestQueue.head.msg)
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

    private def hexDump(bytes: Array[Byte]): String = {
        bytes.map(b => (b & 0xFF).toHexString.toUpperCase.reverse.padTo(2, '0').reverse).mkString(".")
    }

    private def handleResponse(ch: ISOTPChannel, source: Int, message: Array[Byte]): Unit = synchronized {
        // don't handle messages, when we don't have an in-flight request
        if (requestQueue.nonEmpty) {
            val req = requestQueue.head
            if (isMatchingResponse(req.sid, message)) {
                consume()
                req.promise.success(Ok(message))
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

    private def handleIsotpTimeout(source: Int): Unit = synchronized {
        consume().promise.failure(new TimeoutException)
    }

    private def timeoutInflightRequest(): Unit = synchronized {
        consume().promise.success(Error(InternalCauses.Timeout))
    }

    private case class PendingRequest(sid: Byte, msg: Array[Byte], promise: Promise[Result[Array[Byte]]])
}
