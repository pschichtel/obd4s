package tel.schich.obd4s.obd.dtc

sealed abstract class TroubledComponent(val letter: Char)

object TroubledComponent {
    case object Powertrain extends TroubledComponent('P')
    case object Chassis extends TroubledComponent('C')
    case object Body extends TroubledComponent('B')
    case object Network extends TroubledComponent('U')
}
