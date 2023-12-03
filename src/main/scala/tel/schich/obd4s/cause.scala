package tel.schich.obd4s

import tel.schich.obd4s.ObdHelper.{asHex, hexDump}

import scala.collection.immutable.Map

trait Cause {
    def reason: String
}

trait Identified {
    def id: Int
}

case class SimpleCause(reason: String) extends Cause

case class PidMismatch(expected: Int, offset: Int, buf: Array[Byte]) extends Cause {
    val was: Int = buf(offset) & 0xFF
    override def reason: String = s"Response PID did not match request PID: expected=${asHex(expected)} was=${asHex(was)} at=$offset in=${hexDump(buf)}"
}

enum InternalCause(val id: Int, val reason: String) extends Cause with Identified {
    case UnknownCause extends InternalCause(0, "Unknown error!")
    case FilteredAway extends InternalCause(1, "Filter did not apply!")
    case ResponseTooShort extends InternalCause(2, "The response had too little data!")
    case WrongSid extends InternalCause(3, "Successful response, but for the wrong SID!")
    case UnknownResponse extends InternalCause(4, "ECU returned an unknown response!")
    case Timeout extends InternalCause(5, "Request timed out!")
    case ReadError extends InternalCause(7, "A part of the response could not be read!")
}

object InternalCause {
    lazy val lookupByCode: Map[Int, InternalCause] = values.map(c => (c.id, c)).toMap
}

def a(): Unit = {
    InternalCause.values
}


