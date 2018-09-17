package tel.schich.obd4s.obd

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.{Cause, Causes, Identified}

sealed class ObdCause(val id: Int, val reason: String) extends EnumEntry with Cause with Identified

object ObdCauses extends Enum[ObdCause] with Causes[ObdCause] {

    private val ServiceOrSubFunctionNotSupported = "Service or Subfunction not supported (in active Session)"

    case object GeneralReject extends ObdCause(0x10, "General reject")
    case object ServiceOrSubfunctionNotSupportedA extends ObdCause(0x11, ServiceOrSubFunctionNotSupported)
    case object ServiceOrSubfunctionNotSupportedB extends ObdCause(0x12, ServiceOrSubFunctionNotSupported)
    case object MessageLengthOrFormatIncorrect extends ObdCause(0x13, "Message length or format incorrect")
    case object BusyRepeatRequest extends ObdCause(0x21, "Busy - Repeat request")
    case object ConditionsNotCorrect extends ObdCause(0x22, "Conditions not correct")
    case object RequestSequenceError extends ObdCause(0x24, "Request sequence error")
    case object OutOfRange extends ObdCause(0x31, "Out of range")
    case object SecurityAccessDenied extends ObdCause(0x33, "Security access denied")
    case object InvalidKey extends ObdCause(0x35, "Invalid key")
    case object ExceedAttempts extends ObdCause(0x36, "Exceed attempts")
    case object BusyResponsePending extends ObdCause(0x78, "Busy - Response pending")
    case object ServiceOrSubfunctionNotSupportedC extends ObdCause(0x7E, ServiceOrSubFunctionNotSupported)
    case object ServiceOrSubfunctionNotSupportedD extends ObdCause(0x7F, ServiceOrSubFunctionNotSupported)

    override val values = findValues
}
