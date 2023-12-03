package tel.schich.obd4s.elm

import tel.schich.obd4s.{Cause, Identified}

enum ElmCause(val id: Int, val reason: String) extends Cause with Identified {
    case NoData extends ElmCause(1, "No data returned for request")
    case NoResponse extends ElmCause(2, "No response received")
    case UnknownOrInvalidCommand extends ElmCause(3, "Unknown or invalid command")
}
