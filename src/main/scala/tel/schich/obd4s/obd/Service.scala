package tel.schich.obd4s.obd

import enumeratum.{Enum, EnumEntry}

class Service(val id: ServiceId)
sealed abstract class StandardService(id: StandardServiceId) extends Service(id) with EnumEntry

object StandardModes extends Enum[StandardService] {
    case object CurrentData           extends StandardService(StandardServiceId(0x01.toByte))
    case object FreezeFrameData       extends StandardService(StandardServiceId(0x02.toByte))
    case object ShowTroubleCodes      extends StandardService(StandardServiceId(0x03.toByte))
    case object ClearTroubleCodes     extends StandardService(StandardServiceId(0x04.toByte))
    case object TestResultsNonCan     extends StandardService(StandardServiceId(0x05.toByte))
    case object TestResultsCan        extends StandardService(StandardServiceId(0x06.toByte))
    case object PendingTroubleCodes   extends StandardService(StandardServiceId(0x07.toByte))
    case object ControlOperation      extends StandardService(StandardServiceId(0x08.toByte))
    case object VehicleInfo           extends StandardService(StandardServiceId(0x09.toByte))
    case object PermanentTroubleCodes extends StandardService(StandardServiceId(0x0A.toByte))

    val values = findValues

    val lookup: Map[Int, Service] = values.map(m => m.id.id -> m).toMap
}

sealed trait ServiceId {
    def id: Int
    def bytes: Array[Byte]
    def length: Int
    def response: Int = id + 0x40
}

case class StandardServiceId(idByte: Byte) extends ServiceId {
    override val id: Int = idByte
    override lazy val bytes: Array[Byte] = Array(idByte)

    override val length: Int = 1
}

case class ProprietaryServiceId(idShort: Short) extends ServiceId {
    override val id: Int = idShort
    override lazy val bytes: Array[Byte] = {
        val a = (id >>> 8).toByte
        val b = (id & 0xFF).toByte
        Array(a, b)
    }

    override val length: Int = 2
}
