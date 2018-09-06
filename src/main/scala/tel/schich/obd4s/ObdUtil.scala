package tel.schich.obd4s

import tel.schich.obd4s.ObdBridge.SuccessfulResponseBase

object ObdUtil {

    def checkResponse(requestSid: Byte, data: Array[Byte]): Result[Array[Byte]] = {
        // there must be at least the response code, otherwise this is very weird.
        if (data.length > 1) {
            if (requestSid + SuccessfulResponseBase == data(0)) Ok(data)
            else if (requestSid > SuccessfulResponseBase) Error("Successful response, but for the wrong SID!")
            else Error("ECU returned an error!") // TODO add error
        } else {
            Error("The received response was too short! At least 2 bytes are required")
        }
    }

}
