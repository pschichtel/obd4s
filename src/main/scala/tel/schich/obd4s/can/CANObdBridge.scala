package tel.schich.obd4s.can

import com.typesafe.scalalogging.StrictLogging
import tel.schich.javacan.isotp.AggregatingFrameHandler.aggregateFrames
import tel.schich.javacan.isotp.{ISOTPBroker, ISOTPChannel}
import tel.schich.obd4s.obd.{ModeId, Reader}
import tel.schich.obd4s.{ObdBridge, ObdUtil, Result}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class CANObdBridge(broker: ISOTPBroker, ecuAddress: Int)(implicit ec: ExecutionContext) extends ObdBridge with StrictLogging {

    private val channel = broker.createChannel(ecuAddress, aggregateFrames(this.handleResponse))

    private val requestQueue: mutable.Queue[Request[_]] = mutable.Queue()

    override def executeRequest[A](mode: ModeId, pid: Int, reader: Reader[A]): Future[Result[A]] = {
        execAll(mode, Seq(pid)).map { result =>
            result
                .flatMap { buf => reader.read(buf, mode.length) }
                .map(_._1)
        }
    }


    override def executeRequest[A, B](mode: ModeId, a: Req[A], b: Req[B]): Future[Result[(A, B)]] = {
        execAll(mode, Seq(a._1, b._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- a._2.read(buf, mode.length)
                    (br, _) <- b._2.read(buf, oa)
                } yield (ar, br)
            }
        }
    }

    override def executeRequest[A, B, C](mode: ModeId, a: Req[A], b: Req[B], c: Req[C]): Future[Result[(A, B, C)]] = {
        execAll(mode, Seq(a._1, b._1, c._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- a._2.read(buf, mode.length)
                    (br, ob) <- b._2.read(buf, oa)
                    (cr, _) <- c._2.read(buf, ob)
                } yield (ar, br, cr)
            }
        }
    }

    override def executeRequest[A, B, C, D](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D]): Future[Result[(A, B, C, D)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- a._2.read(buf, mode.length)
                    (br, ob) <- b._2.read(buf, oa)
                    (cr, oc) <- c._2.read(buf, ob)
                    (dr, _) <- d._2.read(buf, oc)
                } yield (ar, br, cr, dr)
            }
        }
    }

    override def executeRequest[A, B, C, D, E](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E]): Future[Result[(A, B, C, D, E)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1, e._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- a._2.read(buf, mode.length)
                    (br, ob) <- b._2.read(buf, oa)
                    (cr, oc) <- c._2.read(buf, ob)
                    (dr, od) <- d._2.read(buf, oc)
                    (er, _) <- e._2.read(buf, od)
                } yield (ar, br, cr, dr, er)
            }
        }
    }

    override def executeRequest[A, B, C, D, E, F](mode: ModeId, a: Req[A], b: Req[B], c: Req[C], d: Req[D], e: Req[E], f: Req[F]): Future[Result[(A, B, C, D, E, F)]] = {
        execAll(mode, Seq(a._1, b._1, c._1, d._1, e._1, f._1)).map { result =>
            result.flatMap { buf =>
                for {
                    (ar, oa) <- a._2.read(buf, mode.length)
                    (br, ob) <- b._2.read(buf, oa)
                    (cr, oc) <- c._2.read(buf, ob)
                    (dr, od) <- d._2.read(buf, oc)
                    (er, oe) <- e._2.read(buf, od)
                    (fr, _) <- f._2.read(buf, oe)
                } yield (ar, br, cr, dr, er, fr)
            }
        }
    }

    private def execAll(mode: ModeId, pids: Seq[Int]): Future[Result[Array[Byte]]] = {

        val sidByte = (mode.id & 0xFF).toByte
        val msg = (mode.id +: pids).map(p => (p & 0xFF).toByte).toArray

        val promise = Promise[Result[Array[Byte]]]
        enqueue(Request(sidByte, msg, promise))

        promise.future
    }

    private def enqueue(req: Request[_]): Unit = synchronized {
        val wasEmpty = requestQueue.isEmpty
        requestQueue.enqueue(req)
        if (wasEmpty) {
            sendNext()
        }
    }

    private def sendNext(): Unit = synchronized {
        if (requestQueue.nonEmpty) {
            channel.send(requestQueue.head.msg)
        }
    }

    private def handleResponse(ch: ISOTPChannel, source: Int, message: Array[Byte]): Unit = {
        val req = synchronized {
            val current = requestQueue.dequeue()
            sendNext()
            current
        }
        val result = ObdUtil.checkResponse(req.sid, req.msg)

        req.promise.complete(Success(result))
    }

    private case class Request[A](sid: Byte, msg: Array[Byte], promise: Promise[Result[Array[Byte]]])
}
