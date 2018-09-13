package tel.schich.obd4s.elm

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.{Cause, Causes, Identified}

sealed case class ElmCause(id: Int, reason: String) extends EnumEntry with Cause with Identified

object ElmCauses extends Enum[ElmCause] with Causes[ElmCause] {
    object NoData extends ElmCause(1, "No data returned for request")
    object NoResponse extends ElmCause(2, "No response received")
    object UnknownOrInvalidCommand extends ElmCause(3, "Unknown or invalid command")

    override val values = findValues
}
