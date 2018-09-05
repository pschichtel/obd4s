package tel.schich.obd4s.obd

import boopickle.Default._
import boopickle.{PickleState, PicklerHelper, UnpickleState}
import enumeratum.{Enum, EnumEntry}

class Mode(val id: ModeId)
sealed abstract class StandardMode(id: StandardModeId) extends Mode(id) with EnumEntry

object Mode extends PicklerHelper {
    implicit val pickler: P[Mode] = new P[Mode] {
        override def pickle(obj: Mode)(implicit state: PickleState): Unit = {
            write(obj.id.id)
        }

        override def unpickle(implicit state: UnpickleState): Mode = {
            val id = read[Int]
            StandardModes.lookup.getOrElse(id, new Mode(ProprietaryModeId(id.toShort)))
        }
    }
}

object StandardModes extends Enum[StandardMode] {
    case object CurrentData           extends StandardMode(StandardModeId(0x01.toByte))
    case object FreezeFrameData       extends StandardMode(StandardModeId(0x02.toByte))
    case object ShowTroubleCodes      extends StandardMode(StandardModeId(0x03.toByte))
    case object ClearTroubleCodes     extends StandardMode(StandardModeId(0x04.toByte))
    case object TestOxygenSensor      extends StandardMode(StandardModeId(0x05.toByte))
    case object TestOtherSystem       extends StandardMode(StandardModeId(0x06.toByte))
    case object PendingTroubleCodes   extends StandardMode(StandardModeId(0x07.toByte))
    case object ControlOperation      extends StandardMode(StandardModeId(0x08.toByte))
    case object VehicleInfo           extends StandardMode(StandardModeId(0x09.toByte))
    case object PermanentTroubleCodes extends StandardMode(StandardModeId(0x0A.toByte))

    val values = findValues

    val lookup: Map[Int, Mode] = values.map(m => m.id.id -> m).toMap
}

object ModeId {
    val StandardResponseOffset: Int = 0x40
}

sealed trait ModeId {
    def id: Int
    def bytes: Array[Byte]
    def length: Int
    def response: Int = id + 0x40
}

case class StandardModeId(idByte: Byte) extends ModeId {
    override val id: Int = idByte
    override lazy val bytes: Array[Byte] = Array(idByte)

    override val length: Int = 1
}

case class ProprietaryModeId(idShort: Short) extends ModeId {
    override val id: Int = idShort
    override lazy val bytes: Array[Byte] = {
        val a = (id >>> 8).toByte
        val b = (id & 0xFF).toByte
        Array(a, b)
    }

    override val length: Int = 2
}
