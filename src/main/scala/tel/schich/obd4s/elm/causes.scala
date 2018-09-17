package tel.schich.obd4s.elm

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.{Cause, Causes, Identified}

sealed class ElmCause(val id: Int, val reason: String) extends EnumEntry with Cause with Identified

object ElmCauses extends Enum[ElmCause] with Causes[ElmCause] {
    case object NoData extends ElmCause(1, "No data returned for request")
    case object NoResponse extends ElmCause(2, "No response received")
    case object UnknownOrInvalidCommand extends ElmCause(3, "Unknown or invalid command")

    override val values = findValues
}
