package tel.schich.obd4s

import enumeratum.{Enum, EnumEntry}

sealed case class Cause(code: Int, reason: String) extends EnumEntry

trait Causes { self: Enum[Cause] =>
    lazy val lookupByCode: Map[Int, Cause] =
        values.map(c => (c.code, c)).toMap
}

object InternalCauses extends Enum[Cause] with Causes {
    object UnknownCause extends Cause(0, "Unknown error!")
    object FilteredAway extends Cause(1, "Filter did not apply!")
    object ResponseTooShort extends Cause(2, "Filter did not apply!")
    object WrongSid extends Cause(3, "Successful response, but for the wrong SID!")
    object UnknownResponse extends Cause(4, "ECU returned an unknown response!")
    object Timeout extends Cause(5, "Request timed out!")
    object PidMismatch extends Cause(6, "A response PID did not match the request PID!")
    object ReadError extends Cause(7, "A part of the response could not be read!")

    override val values = findValues
}

object ObdCauses extends Enum[Cause] with Causes {

    private val ServiceOrSubfunctionNotSupported = "Service or Subfunction not supported (in active Session)"

    object GeneralReject extends Cause(0x10, "General reject")
    object ServiceOrSubfunctionNotSupportedA extends Cause(0x11, ServiceOrSubfunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedB extends Cause(0x12, ServiceOrSubfunctionNotSupported)
    object MessageLengthOrFormatIncorrect extends Cause(0x13, "Message length or format incorrect")
    object BusyRepeatRequest extends Cause(0x21, "Busy - Repeat request")
    object ConditionsNotCorrect extends Cause(0x22, "Conditions not correct")
    object RequestSequenceError extends Cause(0x24, "Request sequence error")
    object OutOfRange extends Cause(0x31, "Out of range")
    object SecurityAccessDenied extends Cause(0x33, "Security access denied")
    object InvalidKey extends Cause(0x35, "Invalid key")
    object ExceedAttempts extends Cause(0x36, "Exceed attempts")
    object BusyResponsePending extends Cause(0x78, "Busy - Response pending")
    object ServiceOrSubfunctionNotSupportedC extends Cause(0x7E, ServiceOrSubfunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedD extends Cause(0x7F, ServiceOrSubfunctionNotSupported)

    override val values = findValues
}

object ElmCauses extends Enum[Cause] with Causes {
    object NoData extends Cause(1, "No data returned for request")
    object NoResponse extends Cause(2, "No response received")
    object UnknownOrInvalidCommand extends Cause(3, "Unknown or invalid command")

    override val values = findValues
}

