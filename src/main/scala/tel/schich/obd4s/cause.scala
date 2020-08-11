package tel.schich.obd4s

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.ObdHelper.{asHex, hexDump}

trait Cause {
    def reason: String
}

trait Identified {
    def id: Int
}

case class SimpleCause(reason: String) extends Cause

trait Causes[T <: EnumEntry with Cause with Identified] { self: Enum[T] =>
    lazy val lookupByCode: Map[Int, T] =
        values.map(c => (c.id, c)).toMap
}

case class PidMismatch(expected: Int, offset: Int, buf: Array[Byte]) extends Cause {
    val was: Int = buf(offset) & 0xFF
    override def reason: String = s"Response PID did not match request PID: expected=${asHex(expected)} was=${asHex(was)} at=$offset in=${hexDump(buf)}"
}

sealed class InternalCause(val id: Int, val reason: String) extends EnumEntry with Cause with Identified

object InternalCauses extends Enum[InternalCause] with Causes[InternalCause] {
    case object UnknownCause extends InternalCause(0, "Unknown error!")
    case object FilteredAway extends InternalCause(1, "Filter did not apply!")
    case object ResponseTooShort extends InternalCause(2, "The response had too little data!")
    case object WrongSid extends InternalCause(3, "Successful response, but for the wrong SID!")
    case object UnknownResponse extends InternalCause(4, "ECU returned an unknown response!")
    case object Timeout extends InternalCause(5, "Request timed out!")
    case object ReadError extends InternalCause(7, "A part of the response could not be read!")

    override val values = findValues
}


