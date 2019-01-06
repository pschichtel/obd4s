package tel.schich.obd4s.obd

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.{Cause, Causes, Identified}

sealed class ObdCause(val id: Int, val reason: String) extends EnumEntry with Cause with Identified

/**
  * This enum contains definitions of non-vendor specific error response codes.
  * Nothing of this is based on any standards document, but purely on various websites.
  * Generally the lower the ID is, the more likely it is that the information is correct.
  */
object ObdCauses extends Enum[ObdCause] with Causes[ObdCause] {

    case object GeneralReject extends ObdCause(0x10, "General reject")
    case object ServiceNotSupported extends ObdCause(0x11, "Service Not Supported")
    case object SubFunctionNotSupportedInvalidFormat extends ObdCause(0x12, "Sub Function Not Supported - Invalid Format")
    case object MessageLengthOrFormatIncorrect extends ObdCause(0x13, "Message length or format incorrect")
    case object BusyRepeatRequest extends ObdCause(0x21, "Busy - Repeat request")
    case object ConditionsNotCorrect extends ObdCause(0x22, "Conditions Not Correct Or Request Sequence Error")
    case object RoutineNotCompleteOrServiceInProgress extends ObdCause(0x23, "Routine Not Complete Or Service In Progress")
    case object RequestSequenceError extends ObdCause(0x24, "Request sequence error")
    case object RequestOutOfRange extends ObdCause(0x31, "Request Out Of Range")
    case object SecurityAccessDenied extends ObdCause(0x33, "Security Access Denied - Security Access Requested")
    case object InvalidKey extends ObdCause(0x35, "Invalid key")
    case object ExceedAttempts extends ObdCause(0x36, "Exceed Number Of Attempts")
    case object RequiredTimeDelayNotExpired extends ObdCause(0x37, "Required Time Delay Not Expired")
    case object DownloadNotAccepted extends ObdCause(0x40, "Download Not Accepted")
    case object ImproperDownloadType extends ObdCause(0x41, "Improper Download Type")
    case object CanNotDownloadToSpecifiedAddress extends ObdCause(0x42, "Can Not Download To Specified Address")
    case object CanNotDownloadNumberOfBytesRequested extends ObdCause(0x43, "Can Not Download Number Of Bytes Requested")
    case object UploadNotAccepted extends ObdCause(0x50, "Upload Not Accepted")
    case object ImproperUploadType extends ObdCause(0x51, "Improper Upload Type")
    case object CanNotUploadFromSpecifiedAddress extends ObdCause(0x52, "Can Not Upload From Specified Address")
    case object CanNotUploadNumberOfBytesRequested extends ObdCause(0x53, "Can Not Upload Number Of Bytes Requested")
    case object TransferSuspended extends ObdCause(0x71, "Transfer Suspended")
    case object TransferAborted extends ObdCause(0x72, "Transfer Aborted")
    case object IllegalAddressInBlockTransfer extends ObdCause(0x74, "Illegal Address In Block Transfer")
    case object IllegalByteCountInBlockTransfer extends ObdCause(0x75, "Illegal Byte Count In Block Transfer")
    case object IllegalBlockTrasnferType extends ObdCause(0x76, "Illegal Block Trasnfer Type")
    case object BlockTransferDataChecksumError extends ObdCause(0x77, "Block Transfer Data Checksum Error")
    case object RequestCorrectlyReceivedResponsePending extends ObdCause(0x78, "Request Correctly Received - Response Pending")
    case object IncorrectByteCountDuringBlockTransfer extends ObdCause(0x79, "Incorrect Byte Count During Block Transfer")
    case object ServiceNotSupportedInActiveDiagnosticMode extends ObdCause(0x80, "Service Not Supported In Active Diagnostic Mode")

    override val values = findValues
}
