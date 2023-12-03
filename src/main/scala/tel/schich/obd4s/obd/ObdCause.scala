package tel.schich.obd4s.obd

import tel.schich.obd4s.{Cause, Identified}

/**
  * This enum contains definitions of non-vendor specific error response codes.
  * Nothing of this is based on any standards document, but purely on various websites.
  * Generally the lower the ID is, the more likely it is that the information is correct.
  *
  * @see http://blog.perquin.com/prj/obdii/
  */
enum ObdCause(val id: Int, val reason: String) extends Cause with Identified {
    case GeneralReject extends ObdCause(0x10, "General reject")
    case ServiceNotSupported extends ObdCause(0x11, "Service Not Supported")
    case SubFunctionNotSupportedInvalidFormat extends ObdCause(0x12, "Sub Function Not Supported - Invalid Format")
    case MessageLengthOrFormatIncorrect extends ObdCause(0x13, "Message length or format incorrect")
    case BusyRepeatRequest extends ObdCause(0x21, "Busy - Repeat request")
    case ConditionsNotCorrect extends ObdCause(0x22, "Conditions Not Correct Or Request Sequence Error")
    case RoutineNotCompleteOrServiceInProgress extends ObdCause(0x23, "Routine Not Complete Or Service In Progress")
    case RequestSequenceError extends ObdCause(0x24, "Request sequence error")
    case RequestOutOfRange extends ObdCause(0x31, "Request Out Of Range")
    case SecurityAccessDenied extends ObdCause(0x33, "Security Access Denied - Security Access Requested")
    case InvalidKey extends ObdCause(0x35, "Invalid key")
    case ExceedAttempts extends ObdCause(0x36, "Exceed Number Of Attempts")
    case RequiredTimeDelayNotExpired extends ObdCause(0x37, "Required Time Delay Not Expired")
    case DownloadNotAccepted extends ObdCause(0x40, "Download Not Accepted")
    case ImproperDownloadType extends ObdCause(0x41, "Improper Download Type")
    case CanNotDownloadToSpecifiedAddress extends ObdCause(0x42, "Can Not Download To Specified Address")
    case CanNotDownloadNumberOfBytesRequested extends ObdCause(0x43, "Can Not Download Number Of Bytes Requested")
    case UploadNotAccepted extends ObdCause(0x50, "Upload Not Accepted")
    case ImproperUploadType extends ObdCause(0x51, "Improper Upload Type")
    case CanNotUploadFromSpecifiedAddress extends ObdCause(0x52, "Can Not Upload From Specified Address")
    case CanNotUploadNumberOfBytesRequested extends ObdCause(0x53, "Can Not Upload Number Of Bytes Requested")
    case TransferSuspended extends ObdCause(0x71, "Transfer Suspended")
    case TransferAborted extends ObdCause(0x72, "Transfer Aborted")
    case IllegalAddressInBlockTransfer extends ObdCause(0x74, "Illegal Address In Block Transfer")
    case IllegalByteCountInBlockTransfer extends ObdCause(0x75, "Illegal Byte Count In Block Transfer")
    case IllegalBlockTrasnferType extends ObdCause(0x76, "Illegal Block Trasnfer Type")
    case BlockTransferDataChecksumError extends ObdCause(0x77, "Block Transfer Data Checksum Error")
    case RequestCorrectlyReceivedResponsePending extends ObdCause(0x78, "Request Correctly Received - Response Pending")
    case IncorrectByteCountDuringBlockTransfer extends ObdCause(0x79, "Incorrect Byte Count During Block Transfer")
    case ServiceNotSupportedInActiveDiagnosticMode extends ObdCause(0x80, "Service Not Supported In Active Diagnostic Mode")
}

object ObdCause {
    val NegativeResponseCode: Byte = 0x7F.toByte

    lazy val lookupByCode: Map[Int, ObdCause] = values.map(c => (c.id, c)).toMap
}
