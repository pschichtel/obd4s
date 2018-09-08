package tel.schich.obd4s.elm

import java.io._
import java.net.{InetAddress, Socket}

import com.intel.bluetooth.BluetoothConsts.{PROTOCOL_SCHEME_L2CAP, PROTOCOL_SCHEME_RFCOMM, RFCOMM_CHANNEL_MAX, RFCOMM_CHANNEL_MIN}
import com.typesafe.scalalogging.StrictLogging
import javax.bluetooth.L2CAPConnection
import javax.microedition.io.Connector.READ_WRITE
import javax.microedition.io.{Connector, StreamConnection}
import jssc.SerialPort
import tel.schich.obd4s.ObdBridge

import scala.annotation.tailrec
import scala.util.Random

trait ElmTransport {
    def input: InputStream
    def output: OutputStream

    def unbuffered: ElmTransport = this
    def buffered(bufferSize: Int = 1024): BufferedElmTransport = new BufferedElmTransport(this, bufferSize)

    val AppName = "Scarla"
}

class BufferedElmTransport(slave: ElmTransport, bufferSize: Int) extends ElmTransport {
    override lazy val input: InputStream = new BufferedInputStream(slave.input, bufferSize)
    override lazy val output: OutputStream = new BufferedOutputStream(slave.output, bufferSize)

    override def unbuffered: ElmTransport = slave
    override def buffered(bufferSize: Int): BufferedElmTransport = new BufferedElmTransport(slave, bufferSize)
}

object ElmTransport {
    @tailrec
    def deplete(transport: ElmTransport): Unit = {
        val readable = transport.input.available()
        if (readable > 0) {
            transport.input.skip(readable)
            deplete(transport)
        }
    }
}

class SerialElmTransport(portName: String, baudRate: Int, timeout: Long = 1000) extends ElmTransport with StrictLogging {

    private val port = new SerialPort(portName)
    if (!port.openPort()) {
        throw new IOException("Serial port could not be opened!")
    }
    port.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, true, true)
    logger.info(s"Serial transport initialized: ${port.getPortName}")

    override lazy val input: InputStream = new SerialPortInputStream(port)

    override lazy val output: OutputStream = new SerialPortOutputStream(port)

    private class SerialPortInputStream(port: SerialPort) extends InputStream {
        override def read(): Int = port.readBytes(1)(0)

        override def read(b: Array[Byte]): Int = {
            val data = port.readBytes(b.length)
            System.arraycopy(data, 0, b, 0, data.length)
            data.length
        }

        override def read(b: Array[Byte], off: Int, len: Int): Int = {
            val data = port.readBytes(len)
            System.arraycopy(data, 0, b, off, data.length)
            data.length
        }

        override def available(): Int = port.getInputBufferBytesCount

        override def skip(n: Long): Long = port.readBytes(n.toInt).length
    }

    private class SerialPortOutputStream(port: SerialPort) extends OutputStream {
        override def write(b: Int): Unit = port.writeByte(b.toByte)

        override def write(b: Array[Byte]): Unit = {
            port.writeBytes(b)
            port.getOutputBufferBytesCount
        }

        override def write(b: Array[Byte], off: Int, len: Int): Unit = {
            port.writeBytes(b.slice(off, off + len))
        }

    }

}

class TcpElmTransport(addr: InetAddress, port: Int) extends ElmTransport {

    private val socket = new Socket(addr, port)

    override def input: InputStream =
        socket.getInputStream

    override def output: OutputStream =
        socket.getOutputStream
}

class BluetoothRfcommElmTransport(address: String, channel: Int = 1) extends ElmTransport {
    if ("localhost".equalsIgnoreCase(address)) {
        throw new IllegalArgumentException(s"$address would result in a server socket!")
    }
    if (channel < RFCOMM_CHANNEL_MIN && channel > RFCOMM_CHANNEL_MAX) {
        throw new IllegalArgumentException(s"channel must be within $RFCOMM_CHANNEL_MIN and $RFCOMM_CHANNEL_MAX!")
    }

    private val compatibleAddress = address.replaceAll(":", "")
    private val url = s"$PROTOCOL_SCHEME_RFCOMM://$compatibleAddress:$channel"
    private val connection = Connector.open(url, READ_WRITE) match {
        case c: StreamConnection => c
        case c => throw new IllegalArgumentException(s"Bluetooth connector ${c.getClass.getName} is not compatible!")
    }

    override lazy val input: InputStream = connection.openDataInputStream()

    override lazy val output: OutputStream = connection.openDataOutputStream()
}

class BluetoothL2CAPElmTranport(address: String, channel: Int) extends ElmTransport {
    if ("localhost".equalsIgnoreCase(address)) {
        throw new IllegalArgumentException(s"$address would result in a server socket!")
    }
    if (channel < 0) {
        throw new IllegalArgumentException(s"channel must be positive!")
    }

    private val compatibleAddress = address.replaceAll(":", "")
    private val url = s"$PROTOCOL_SCHEME_L2CAP://$compatibleAddress:${channel.toHexString}"
    private val connection = Connector.open(url, READ_WRITE) match {
        case c: L2CAPConnection => c
        case c => throw new IllegalArgumentException(s"Bluetooth connector ${c.getClass.getName} is not compatible!")
    }


    override lazy val input: InputStream = new InputStream {

        override def read(): Int = {
            val buf = Array.ofDim[Byte](1)
            val read = connection.receive(buf)
            if (read <= 0) -1
            else buf(0).toInt
        }

        override def read(b: Array[Byte]): Int = connection.receive(b)

        override def read(b: Array[Byte], off: Int, len: Int): Int = {
            val buf = Array.ofDim[Byte](len)
            val read = connection.receive(buf)
            System.arraycopy(buf, 0, b, off, read)
            read
        }

        override def available(): Int = {
            if (connection.ready()) 1
            else 0
        }
    }

    override lazy val output: OutputStream = new OutputStream {
        override def write(b: Int): Unit = write(Array(b.toByte))

        override def write(b: Array[Byte]): Unit = connection.send(b)

        override def write(b: Array[Byte], off: Int, len: Int): Unit = {
            val buf = Array.ofDim[Byte](len)
            System.arraycopy(b, off, buf, 0, len)
            connection.send(buf)
        }
    }
}

class DiscardingOutputRandomInputElmTransport(readLatency: Long) extends ElmTransport {

    private val rand = new Random()
    private var lastMode: Int = _
    private var lastPid: Int = _
    private var atCommand: Boolean = false
    private var readState: Int = 1
    private var writeState: Int = 1

    override val input: InputStream = new InputStream {
        override def read(): Int =  {
            if (atCommand) readAt()
            else readObd()
        }

        private def readAt(): Int = {
            readState match {
                case 1 =>
                    readState += 1
                    'O'.toInt
                case 2 =>
                    readState += 1
                    'K'.toInt
                case 3 =>
                    readState += 1
                    '\r'.toInt
                case 4 =>
                    readState += 1
                    Thread.sleep(readLatency)
                    writeState = 1
                    '>'.toInt
                case _ => -1
            }
        }

        private def readObd(): Int = {
            val mode = f"${lastMode + ObdBridge.PositiveResponseBase}%02X"
            val pid = f"$lastPid%02X"
            readState match {
                case 1 =>
                    readState += 1
                    mode(0).toInt
                case 2 =>
                    readState += 1
                    mode(1).toInt
                case 3 =>
                    readState += 1
                    pid(0).toInt
                case 4 =>
                    readState += 1
                    pid(1).toInt
                case n if n > 4 && n < 13 =>
                    readState += 1
                    if (lastPid % 0x20 == 0) 'F'.toInt
                    else rand.nextInt(16).toHexString.head.toInt
                case 13 =>
                    readState += 1
                    '\r'.toInt
                case 14 =>
                    readState += 1
                    Thread.sleep(readLatency)
                    writeState = 1
                    '>'.toInt
                case _ => -1
            }
        }

        override def available(): Int = 0

        override def skip(n: Long): Long = 0
    }

    override val output: OutputStream = new OutputStream {

        private var first: Char = _

        override def write(b: Int): Unit = {
            writeState match {
                case 1 =>
                    writeState += 1
                    first = b.toChar.toUpper
                case 2 =>
                    writeState += 1
                    val second = b.toChar.toUpper
                    atCommand = first == 'A' && second == 'T'
                    if (!atCommand) {
                        lastMode = java.lang.Integer.parseInt("" + first + second, 16)
                    }
                    readState = 1
                case 3 if ! atCommand =>
                    writeState += 1
                    first = b.toChar
                case 4 if ! atCommand =>
                    writeState += 1
                    lastPid = java.lang.Integer.parseInt("" + first + b.toChar, 16)
                case _ =>
            }
        }
    }

}
