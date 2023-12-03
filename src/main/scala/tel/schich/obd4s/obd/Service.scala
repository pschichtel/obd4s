package tel.schich.obd4s.obd

import scala.collection.immutable.Map

class Service(val id: ServiceId)

enum StandardMode(id: StandardServiceId) extends Service(id) {
    case CurrentData           extends StandardMode(StandardServiceId(0x01.toByte))
    case FreezeFrameData       extends StandardMode(StandardServiceId(0x02.toByte))
    case ShowTroubleCodes      extends StandardMode(StandardServiceId(0x03.toByte))
    case ClearTroubleCodes     extends StandardMode(StandardServiceId(0x04.toByte))
    case TestResultsNonCan     extends StandardMode(StandardServiceId(0x05.toByte))
    case TestResultsCan        extends StandardMode(StandardServiceId(0x06.toByte))
    case PendingTroubleCodes   extends StandardMode(StandardServiceId(0x07.toByte))
    case ControlOperation      extends StandardMode(StandardServiceId(0x08.toByte))
    case VehicleInfo           extends StandardMode(StandardServiceId(0x09.toByte))
    case PermanentTroubleCodes extends StandardMode(StandardServiceId(0x0A.toByte))

}

object StandardMode {
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
