package tel.schich.obd4s

import tel.schich.obd4s.ObdBridge.SuccessfulResponseBase

object ObdUtil {

    val NegativeResponseCode: Byte = 0x7F.toByte

    def checkResponse(requestSid: Byte, data: Array[Byte]): Result[Array[Byte]] = {
        // there must be at least the response code, otherwise this is very weird.
        if (data.length > 1) {
            if (requestSid + SuccessfulResponseBase == data(0)) Ok(data)
            else if (requestSid > SuccessfulResponseBase) Error("Successful response, but for the wrong SID!")
            else if (requestSid == NegativeResponseCode) parseNegativeResponse(data)
            else Error("ECU returned an unknown response!") // TODO add error
        } else {
            Error("The received response was too short! At least 2 bytes are required")
        }
    }

    def parseNegativeResponse[T](buf: Array[Byte]): Result[T] = {
        // TODO introduce some kind of error enum
        val reason = buf(1) match {
            case 0x10 => "General reject"
            case 0x11 => "Service or Subfunction not supported (in active Session)"
            case 0x11 => "Service or Subfunction not supported (in active Session)"
            case 0x7E => "Service or Subfunction not supported (in active Session)"
            case 0x7F => "Service or Subfunction not supported (in active Session)"
            case 0x13 => "Message length or format incorrect"
            case 0x31 => "Out of range"
            case 0x21 => "Busy - Repeat request"
            case 0x78 => "Busy - Response pending"
            case 0x22 => "Conditions not correct"
            case 0x24 => "Request sequence error"
            case 0x33 => "Security access denied"
            case 0x35 => "Invalid key"
            case 0x36 => "Exceed attempts"
        }
        Error(reason)
    }

}
