package tel.schich.obd4s.obd

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.{Cause, Causes, Identified}

sealed case class ObdCause(id: Int, reason: String) extends EnumEntry with Cause with Identified

object ObdCauses extends Enum[ObdCause] with Causes[ObdCause] {

    private val ServiceOrSubFunctionNotSupported = "Service or Subfunction not supported (in active Session)"

    object GeneralReject extends ObdCause(0x10, "General reject")
    object ServiceOrSubfunctionNotSupportedA extends ObdCause(0x11, ServiceOrSubFunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedB extends ObdCause(0x12, ServiceOrSubFunctionNotSupported)
    object MessageLengthOrFormatIncorrect extends ObdCause(0x13, "Message length or format incorrect")
    object BusyRepeatRequest extends ObdCause(0x21, "Busy - Repeat request")
    object ConditionsNotCorrect extends ObdCause(0x22, "Conditions not correct")
    object RequestSequenceError extends ObdCause(0x24, "Request sequence error")
    object OutOfRange extends ObdCause(0x31, "Out of range")
    object SecurityAccessDenied extends ObdCause(0x33, "Security access denied")
    object InvalidKey extends ObdCause(0x35, "Invalid key")
    object ExceedAttempts extends ObdCause(0x36, "Exceed attempts")
    object BusyResponsePending extends ObdCause(0x78, "Busy - Response pending")
    object ServiceOrSubfunctionNotSupportedC extends ObdCause(0x7E, ServiceOrSubFunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedD extends ObdCause(0x7F, ServiceOrSubFunctionNotSupported)

    override val values = findValues
}
