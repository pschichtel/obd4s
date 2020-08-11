package tel.schich.obd4s.obd.dtc

import tel.schich.obd4s.obd.dtc.TroubledComponent.{Body, Chassis, Network, Powertrain}

case class DiagnosticTroubleCode(component: TroubledComponent, code: String) {
    override def toString: String = component.letter.toString + code
}

object DiagnosticTroubleCode {

    private val ComponentMask = 0xC000
    private val AMask = 0x3000
    private val BMask = 0x0F00
    private val CMask = 0x00F0
    private val DMask = 0x000F

    def apply(first: Byte, second: Byte): DiagnosticTroubleCode =
        apply((((first & 0xFF) << 8) | (second & 0xFF)).toShort)

    def apply(raw: Short): DiagnosticTroubleCode = {
        val component = (raw & ComponentMask) >> 14 match {
            case 0 => Powertrain
            case 1 => Chassis
            case 2 => Body
            case 3 => Network
        }

        val first = ((raw & AMask) >> 12).toString
        val second = ((raw & BMask) >> 8).toHexString
        val third = ((raw & CMask) >> 4).toHexString
        val forth = (raw & DMask).toHexString

        DiagnosticTroubleCode(component, first + second + third + forth)
    }
}
