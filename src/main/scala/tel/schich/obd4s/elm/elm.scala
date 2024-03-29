package tel.schich.obd4s.elm

import java.io.InputStream
import java.nio.charset.StandardCharsets.US_ASCII

import com.typesafe.scalalogging.StrictLogging
import tel.schich.obd4s.InternalCause.{ResponseTooShort, UnknownResponse}
import tel.schich.obd4s._
import tel.schich.obd4s.elm.ElmCommands.CANReceiveFilter
import tel.schich.obd4s.obd.{ServiceId, PlainRequest, Reader}

import scala.annotation.tailrec
import scala.collection.IndexedSeqView
import scala.concurrent.{ExecutionContext, Future}

class ELMObdBridge(transport: ElmTransport, executionContext: ExecutionContext) extends ObdBridge with StrictLogging {

    private implicit val ec: ExecutionContext = executionContext

    private val Prompt = '>'.toByte
    private val Cr = '\r'.toByte

    initializeController()

    private def initializeController(): Unit = {
        logger.info("Initializing controller...")
        // 1. reset the controller
        logger.debug(executeCommand(ElmCommands.Reset).toString)
        // 2. disable terminal features
        logger.debug(executeCommand(ElmCommands.Echo(false)).toString)
        logger.debug(executeCommand(ElmCommands.Spaces(false)).toString)
        logger.debug(executeCommand(ElmCommands.Linefeed(false)).toString)
        logger.info("done!")
    }

    def selectECU(id: String): Unit = {
        executeCommand(CANReceiveFilter(id))
    }

    def readLines(in: InputStream): Vector[String] = {
        @tailrec
        def doRead(lineBuffer: Vector[Byte], lines: Vector[String]): Vector[String] = {
            in.read() match {
                case n if n == -1 || n == Prompt =>
                    if (lineBuffer.isEmpty) lines
                    else lines :+ new String(lineBuffer.toArray, US_ASCII)
                case Cr =>
                    doRead(Vector.empty, lines :+ new String((lineBuffer :+ Cr).toArray, US_ASCII))
                case n => doRead(lineBuffer :+ n.toByte, lines)
            }
        }

        doRead(Vector.empty, Vector.empty)
    }

    private def parseObdResponse(line: String): Result[(Int, Int, IndexedSeqView[Byte])] = {
        val plainHex = line.filterNot(_.isWhitespace)
        val data = (plainHex.indices by 2).map(i => java.lang.Short.parseShort("" + plainHex(i) + plainHex(i + 1), 16).toByte).toArray
        if (data.length >= 2) Ok((data(0) & 0xFF, data(1) & 0xFF, data.view.slice(2, data.length)))
        else Error(ResponseTooShort)
    }

    private def parseElmResponse(lines: Vector[String], service: ServiceId, pid: Option[Int]): Result[Vector[(String, String)]] = {
        val expectedResponseMode = service.response
        val responsePrefix = obdRequest(expectedResponseMode, pid)

        lines.map(_.toLowerCase) match {
            case Vector() => Error(ElmCause.NoResponse)
            case Vector("?") => Error(ElmCause.UnknownOrInvalidCommand)
            case Vector("no data") => Error(ElmCause.NoData)
            case _ if lines.forall(_.length >= 2) =>
                Ok(lines.map(l => ("", l)))
            case _ => Error(UnknownResponse)

        }
    }

    private def processResponse[T](line: String, expectedMode: Int, pid: Int, reader: Reader[T]): Result[T] = {
        parseObdResponse(line) flatMap {
            case (responseMode, responsePid, data) =>
                if (responseMode == expectedMode && responsePid == pid) {
                    reader.read(data, 0).map(_._1)
                } else {
                    Error(UnknownResponse)
                }
        }
    }

    private def padHex(hex: String) = "0" * (hex.length & 1) + hex
    private def toHex(i: Int): String = padHex(i.toHexString)
    private def obdRequest(service: Int, parameter: Option[Int]) = parameter match {
        case Some(pid) => s"${toHex(service)}${toHex(pid)}"
        case None => s"${toHex(service)}"
    }
    private def isSupportPid(pid: Int) = pid % ObdBridge.SupportRangeSize == 0
    private def optimizedRequest(mode: Int, pid: Option[Int]) = {
        val request = obdRequest(mode, pid)
        val context = (mode, pid)
        s"${request}1" // TODO replace constant 1 by proper calculation
    }


    override def executeRequest[T](service: ServiceId, reader: Reader[T]): Future[Result[T]] = {
        val request = obdRequest(service.id, None)
        val rawResponse = run(transport, request)
        processResponse(rawResponse, service, None, reader)
    }

    override def executeRequest[T](service: ServiceId, parameter: Int, reader: Reader[T]): Future[Result[T]] = {
        val request = optimizedRequest(service.id, Some(parameter))
        val rawResponse = run(transport, request)
        processResponse(rawResponse, service, Some(parameter), reader)
    }

    private def processResponse[T](rawResponse: Vector[String], service: ServiceId, parameter: Option[Int], reader: Reader[T]): Future[Result[T]] = {
        val parsedResponses = parseElmResponse(rawResponse, service, parameter)

        val result = parsedResponses.flatMap {
            case Vector((_, payload), _*) =>
                parseObdResponse(payload).flatMap {
                    case (_, _, data) => reader.read(data, 0).map(_._1)
                }

            case _ => Error(InternalCause.ReadError)
        }

        Future.successful(result)
    }


    override def executeRequest[A, B](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B]): Future[Result[(A, B)]] = ???

    override def executeRequest[A, B, C](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C]): Future[Result[(A, B, C)]] = ???

    override def executeRequest[A, B, C, D](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D]): Future[Result[(A, B, C, D)]] = ???

    override def executeRequest[A, B, C, D, E](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E]): Future[Result[(A, B, C, D, E)]] = ???

    override def executeRequest[A, B, C, D, E, F](service: ServiceId, a: PlainRequest[A], b: PlainRequest[B], c: PlainRequest[C], d: PlainRequest[D], e: PlainRequest[E], f: PlainRequest[F]): Future[Result[(A, B, C, D, E, F)]] = ???

    override def executeRequests[A](service: ServiceId, reqs: Seq[PlainRequest[_ <: A]]): Future[Result[Seq[_ <: A]]] = ???

    private def run(transport: ElmTransport, cmd: String, readDelay: Long = 0): Vector[String] = synchronized {
        ElmTransport.deplete(transport)
        val elmCommand = s"$cmd\r"
        logger.trace("Request: >>>>>>>>>>>")
        logger.trace(elmCommand)
        logger.trace("<<<<<<<<<<<")
        val payload = elmCommand.getBytes(US_ASCII)
        transport.output.write(payload)
        transport.output.flush()
        if (readDelay > 0) {
            Thread.sleep(readDelay)
        }

        val responseLines = readLines(transport.input)
        logger.trace("Response: >>>>>>>>>>>")
        logger.trace(responseLines.map(ELMObdBridge.stringify).mkString("\n"))
        logger.trace("<<<<<<<<<<<")

        val filtered = filterResponse(elmCommand, responseLines)
        logger.trace("Response(filtered): >>>>>>>>>>>")
        logger.trace(filtered.mkString("\n"))
        logger.trace("<<<<<<<<<<<")

        filtered
    }

    private def filterResponse(command: String, response: Vector[String]): Vector[String] = {
        // drop the echo line, trim whitespace from other lines and finally remove empty ones
        response.filterNot(_ == command).map(_.trim).filter(_.nonEmpty)
    }

    def executeCommand(cmd: ElmCommand): Option[String] = {
        run(transport, cmd.command, cmd.delay) match {
            case Vector(head, _*) => Some(head)
            case _ => None
        }
    }
}

object ELMObdBridge {
    def stringify(byte: Int): String = {
        byte match {
            case n if n < 0 => "<EOF>"
            case n if n >= 32 => s"${byte.toChar}"
            case 0 => "\\0"
            case '\r' => "\\r"
            case '\n' => "\\n"
            case '\t' => "\\t"
            case '\\' => "\\\\"
            case n => s"[$n]"
        }
    }

    def stringify(s: Seq[Int]): String =
        s.map(stringify).mkString("")

    def stringify(s: String): String =
        s.map(c => stringify(c.toInt)).mkString("")
}

sealed trait ElmCommand {
    def command: String = "AT"
    def delay: Long
}

sealed abstract class SimpleElmCommand(val cmd: String, val delay: Long = 0) extends ElmCommand {
    override def command: String = super.command + cmd
}

sealed abstract class SwitchElmCommand(cmd: String, on: Boolean, delay: Long = 0) extends SimpleElmCommand(cmd, delay) {
    override def command: String = super.command + (if (on) "1" else "0")
}

object ElmCommands {
    case object Reset                         extends SimpleElmCommand("Z", 1000)
    case object FastReset                     extends SimpleElmCommand("WS")
    case class Linefeed(on: Boolean)          extends SwitchElmCommand("L", on)
    case class Echo(on: Boolean)              extends SwitchElmCommand("E", on)
    case class Spaces(on: Boolean)            extends SwitchElmCommand("S", on)
    case class Headers(on: Boolean)           extends SwitchElmCommand("H", on)
    case object SetDefaults                   extends SimpleElmCommand("D")
    case object VersionId                     extends SimpleElmCommand("I")
    case object ReadVoltage                   extends SimpleElmCommand("RV")
    case object ReadIgnMon                    extends SimpleElmCommand("IGN")
    case object LowPower                      extends SimpleElmCommand("LP")
    case object CurrentProtocol               extends SimpleElmCommand("DP")
    case object CurrentProtocolNumber         extends SimpleElmCommand("DPN")
    case object ErrorCounts                   extends SimpleElmCommand("CS")
    case object DumpBuffer                    extends SimpleElmCommand("BD")
    case object DeviceDescription             extends SimpleElmCommand("@1")
    case class CANReceiveFilter(addr: String) extends SimpleElmCommand("CRA" + addr)

    case class AdaptiveTiming(mode: AdaptiveTiming.Mode) extends SimpleElmCommand("AT") {
        override def command: String = super.command + mode.id
    }
    object AdaptiveTiming {
        sealed abstract class Mode(val id: Int)
        case object Off extends Mode(0)
        case object Normal extends Mode(1)
        case object Aggressive extends Mode(3)
    }

}
