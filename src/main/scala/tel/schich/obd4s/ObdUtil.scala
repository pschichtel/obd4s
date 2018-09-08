package tel.schich.obd4s

import tel.schich.obd4s.ObdBridge.PositiveResponseBase

object ObdUtil {

    val NegativeResponseCode: Byte = 0x7F.toByte

    def checkResponse(requestSid: Byte, data: Array[Byte]): Result[Array[Byte]] =
        // there must be at least the response code, otherwise this is very weird.
        if (isMatchingResponse(requestSid, data)) Ok(data)
        else if (isPositiveResponse(data)) Error(Causes.WrongSid)
        else getErrorCause(data) match {
            case Some(cause) => Error(cause)
            case _ => Error(Causes.UnknownResponse)
        }

    def isMatchingResponse(requestSid: Byte, data: Array[Byte]): Boolean =
        isPositiveResponse(data) && (data(0) + PositiveResponseBase) == requestSid

    def isPositiveResponse(data: Array[Byte]): Boolean =
        data.nonEmpty && data(0) >= PositiveResponseBase

    def isErrorResponse(data: Array[Byte]): Boolean =
        data.nonEmpty && data(0) == NegativeResponseCode

    def getErrorCause(data: Array[Byte]): Option[Cause] =
        if (!isErrorResponse(data) || data.length < 2) None
        else Some(Causes.lookupByCode.getOrElse(data(1), Causes.UnknownCause))
}
