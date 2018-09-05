package tel.schich.obd4s.obd

import java.util.concurrent.TimeUnit.SECONDS

import com.typesafe.scalalogging.StrictLogging
import tel.schich.obd4s.obd.DistanceReader.DistanceUnit
import tel.schich.obd4s.obd.FuelSystemStatus.Unavailable
import tel.schich.obd4s.{Failure, Ok, Result}

import scala.concurrent.duration.TimeUnit

case class Temperature(temperature: Double) extends Response {
    override def values() = Map("temperature" -> FloatValue(temperature))
}

object TemperatureReader extends SingleByteReader[Temperature] {
    override def read(a: Int): Result[Temperature] = Ok(Temperature(a - 40))
}

case class RPM(rpm: Double) extends Response {
    override def values() = Map("rpm" -> FloatValue(rpm))
}

object RpmReader extends SingleShortReader[RPM] {
    override def read(ab: Int): Result[RPM] = Ok(RPM(ab / 4.0))
}

case class BitSet(set: Array[Boolean]) extends Response {
    override def values(): Map[String, StringValue] = {
        val bitString = set.reverseIterator.map {
            case true => '1'
            case false => '0'
        }.mkString
        Map("bits" -> StringValue(bitString))
    }
}

class BitSetReader(n: Int, msbToLsb: Boolean, merger: (Boolean, Boolean) => Boolean) extends FixedLengthReader[BitSet](n) with StrictLogging {

    override def read(buf: BufferView): Result[BitSet] = {
        val (bytes, bits) =
            if (msbToLsb) (buf.iterator, 7 to 0 by -1)
            else (buf.reverseIterator, 0 to 7)
        val set = bytes.map(_ & 0xFF).flatMap(byte => bits.map(bit => ((byte >>> bit) & 1) == 1)).toArray
        logger.trace(set.map(b => if (b) '1' else '0').mkString)
        Ok(BitSet(set))
    }

    override def merge(left: BitSet, right: BitSet): BitSet =
        BitSet(left.set.zip(right.set).map(merger.tupled))
}

object PidSupportReader extends BitSetReader(4, true, _ || _)

case class FuelType(id: Int, name: String) extends Response {
    override def values(): Map[String, Value] = Map(
        "fuel_id"   -> EnumarableValue(IntegerValue(id)),
        "fuel_name" -> StringValue(name)
    )
}

object FuelTypeReader extends SingleByteReader[FuelType] {
    val KnownFuels = Map(
        0x01 -> "Gasoline",
        0x02 -> "Methanol",
        0x03 -> "Ethanol",
        0x04 -> "Diesl",
        0x05 -> "Liquefied Petroleum Gas",
        0x06 -> "Compressed Natural Gas",
        0x07 -> "Propane",
        0x08 -> "Electric",
        0x09 -> "Bifuel running Gasoline",
        0x0A -> "Bifuel running Methanol",
        0x0B -> "Bifuel running Ethanol",
        0x0C -> "Bifuel running Liquefied Petroleum Gas",
        0x0D -> "Bifuel running Compressed Natural Gas",
        0x0E -> "Bifuel running Propane",
        0x0F -> "Bifuel running Electricity",
        0x10 -> "Bifuel running electric and combustion engine",
        0x11 -> "Hybrid gasoline",
        0x12 -> "Hybrid Ethanol",
        0x13 -> "Hybrid Diesel",
        0x14 -> "Hybrid Electric",
        0x15 -> "Hybrid running electric and combustion engine",
        0x16 -> "Hybrid Regenerative",
        0x17 -> "Bifuel running diesel"

    )

    override def read(a: Int): Result[FuelType] = {
        a match {
            case n if n <= 0 => Failure(s"Invalid fuel type id $n!")
            case n if KnownFuels.isDefinedAt(n) => Ok(FuelType(n, KnownFuels(n)))
            case n => Ok(FuelType(n, "Unknown"))
        }
    }
}

case class EngineLoad(load: Double) extends Response {
    override def values(): Map[String, Value] = Map("load" -> FloatValue(load))
}

object EngineLoadReader extends SingleByteReader[EngineLoad] {
    override def read(a: Int): Result[EngineLoad] = {
        Ok(EngineLoad(a / 255.0))
    }
}

case class FuelSystemStatus(first: FuelSystemStatus.Status, second: FuelSystemStatus.Status) extends Response {
    override def values(): Map[String, Value] = Map(
        "first"  -> IntegerValue(first.id),
        "second" -> IntegerValue(second.id)
    )
}
object FuelSystemStatus {
    sealed abstract class Status(val id: Int)
    case object Unavailable extends Status(0)
    case object OpenLoopEngineTemperature extends Status(1)
    case object ClosedLoopOxygenSensors extends Status(2)
    case object OpenLoopEngineLoad extends Status(4)
    case object OpenLoopSystemFailure extends Status(8)
    case object ClosedLoopFeedbackFault extends Status(16)
}
object FuelSystemStatusReader extends TwoByteReader[FuelSystemStatus] {

    private def toStatus(i: Int): FuelSystemStatus.Status = {
        i match {
            case 1 => FuelSystemStatus.OpenLoopEngineTemperature
            case 2 => FuelSystemStatus.ClosedLoopOxygenSensors
            case 4 => FuelSystemStatus.OpenLoopEngineLoad
            case 8 => FuelSystemStatus.OpenLoopSystemFailure
            case 16 => FuelSystemStatus.ClosedLoopFeedbackFault
            case _ => FuelSystemStatus.Unavailable
        }
    }

    override def read(a: Int, b: Int): Result[FuelSystemStatus] = {
        toStatus(a) match {
            case Unavailable => Failure(s"Fuel system 1 returned an undefined response: $a")
            case statusA => Ok(FuelSystemStatus(statusA, toStatus(b)))
        }

    }
}

case class VehicleSpeed(speed: Int) extends Response {
    override def values(): Map[String, Value] = Map("speed" -> IntegerValue(speed))
}
object VehicleSpeedReader extends SingleByteReader[VehicleSpeed] {
    override def read(a: Int): Result[VehicleSpeed] = {
        Ok(VehicleSpeed(a))
    }
}

case class RelativeValue(position: Double) extends Response {
    override def values(): Map[String, Value] = Map("value" -> FloatValue(position))
}
object SingleBytePercentageReader extends SingleByteReader[RelativeValue] {
    override def read(a: Int): Result[RelativeValue] = Ok(RelativeValue(a / 255.0))
}
object SingleByteSignedPercentageReader extends SingleByteReader[RelativeValue] {
    override def read(a: Int): Result[RelativeValue] = Ok(RelativeValue((a * 2 / 255.0) - 1))
}

case class OxygenSensorFuelVoltageTrim(voltage: Double, fuelTrim: Double) extends Response {
    override def values(): Map[String, Value] = Map(
        "voltage"   -> FloatValue(voltage),
        "fuel_trim" -> FloatValue(fuelTrim)
    )
}
object OxygenSensorFuelVoltageReader extends TwoByteReader[OxygenSensorFuelVoltageTrim] {
    override def read(a: Int, b: Int): Result[OxygenSensorFuelVoltageTrim] = {
        if (b == 255) Failure(s"Oxygen sensor response signaled no support: $b")
        else Ok(OxygenSensorFuelVoltageTrim(a / 200.0, 100.0/128.0 * b - 100.0))
    }
}

case class Duration(seconds: Long) extends Response {
    override def values(): Map[String, Value] = Map("seconds" -> IntegerValue(seconds))
}
case class RuntimeReader(unit: TimeUnit) extends SingleShortReader[Duration] {
    override def read(ab: Int): Result[Duration] = {
        Ok(Duration(SECONDS.convert(ab, unit)))
    }
}

case class TimingAdvance(advance: Double) extends Response {
    override def values(): Map[String, Value] = Map("advance" -> FloatValue(advance))
}
object TimingAdvanceReader extends SingleByteReader[TimingAdvance] {
    override def read(a: Int): Result[TimingAdvance] = {
        Ok(TimingAdvance((a/2.0) - 40))
    }
}

case class OxygenSensorFuelAirVoltage(fuelAirEquivRatio: Double, voltage: Double) extends Response {
    override def values(): Map[String, Value] = Map(
        "fuel_air_equiv_ratio" -> FloatValue(fuelAirEquivRatio),
        "voltage"              -> FloatValue(voltage)
    )
}
object OxygenSensorFuelAirVoltageReader extends TwoShortReader[OxygenSensorFuelAirVoltage] {
    override def read(a: Int, b: Int): Result[OxygenSensorFuelAirVoltage] = {
        Ok(OxygenSensorFuelAirVoltage(FuelAirEquivalenceRatioReader.ratio(a), (8.0/65536.0) * b))
    }
}
case class OxygenSensorFuelAirCurrent(fuelAirEquivRatio: Double, current: Double) extends Response {
    override def values(): Map[String, Value] = Map(
        "fuel_air_equiv_ratio" -> FloatValue(fuelAirEquivRatio),
        "current"              -> FloatValue(current)
    )
}
object OxygenSensorFuelAirCurrentReader extends TwoShortReader[OxygenSensorFuelAirCurrent] {
    override def read(ab: Int, cd: Int): Result[OxygenSensorFuelAirCurrent] = {
        Ok(OxygenSensorFuelAirCurrent(FuelAirEquivalenceRatioReader.ratio(ab), Current.mA2A((cd/256.0) - 128)))
    }
}

object Current {
    def mA2A(n: Double): Double = n / 1000.0
}

object CatalystTemperatureReader extends SingleShortReader[Temperature] {
    override def read(ab: Int): Result[Temperature] = {
        Ok(Temperature((ab/10.0) - 10))
    }
}

case class Pressure(pressure: Double) extends Response {
    override def values(): Map[String, Value] = Map("pressure" -> FloatValue(pressure))
}
object Pressure {
    def fromKiloPa(pressure: Double): Pressure = Pressure(kPa2Pa(pressure))
    def kPa2Pa(n: Double): Double = n * 1000
}
object BarometricPressureReader extends SingleByteReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(a))
}

case class Distance(distance: Int) extends Response {
    override def values(): Map[String, Value] = Map("distance" -> IntegerValue(distance))
}
case class DistanceReader(unit: DistanceUnit) extends SingleShortReader[Distance] {
    override def read(a: Int): Result[Distance] = Ok(Distance(a * unit.factor))
}
object DistanceReader {
    sealed abstract class DistanceUnit(val factor: Int)

    case object Meter extends DistanceUnit(1)
    case object KiloMeter extends DistanceUnit(1000)
}

case class Voltage(voltage: Double) extends Response {
    override def values(): Map[String, Value] = Map("voltage" -> FloatValue(voltage))
}
object ControlModuleVoltageReader extends TwoByteReader[Voltage] {
    override def read(a: Int, b: Int): Result[Voltage] = Ok(Voltage((256 * a + b)/1000.0))
}
case class FuelAirEquivalenceRatio(ratio: Double) extends Response {
    override def values(): Map[String, Value] = Map("ratio" -> FloatValue(ratio))
}
object FuelAirEquivalenceRatioReader extends SingleShortReader[FuelAirEquivalenceRatio] {
    def ratio(n: Int): Double = (2.0 / 65536.0) * n

    override def read(a: Int): Result[FuelAirEquivalenceRatio] =
        Ok(FuelAirEquivalenceRatio(ratio(a)))
}

case class FuelRate(rate: Double) extends Response {
    override def values(): Map[String, Value] = Map("rate" -> FloatValue(rate))
}
object FuelRateReader extends SingleShortReader[FuelRate] {
    override def read(a: Int): Result[FuelRate] = Ok(FuelRate(a / 20.0))
}

case class SecondaryOxygenSensorTrim(trim: (Double, Double)) extends Response {
    override def values(): Map[String, Value] = Map(
        "trim_a" -> FloatValue(trim._1),
        "trim_b" -> FloatValue(trim._2)
    )
}
object SecondaryOxygenSensorTrimReader extends TwoByteReader[SecondaryOxygenSensorTrim] {
    def trim(n: Int): Double = ((100.0/128.0) * n) - 100

    override def read(a: Int, b: Int): Result[SecondaryOxygenSensorTrim] = {
        Ok(SecondaryOxygenSensorTrim((trim(a), trim(b))))
    }
}

object FuelRailPressureReader extends SingleShortReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(10 * a))
}

object FuelPressureReader extends SingleByteReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(3 * a))
}

object IntakeManifoldPressureReader extends SingleByteReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(a))
}

object FuelRailPressureRelativeToManifoldReader extends SingleShortReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(0.079 * a))
}

object FuelRailGaugePressureReader extends SingleShortReader[Pressure] {
    override def read(a: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(10 * a))
}

object SystemVaporPressureReader extends SingleShortReader[Pressure] {
    override def read(ab: Int): Result[Pressure] = Ok(Pressure(ab.toShort / 4.0))
}

object EvapSystemVaporPressureReader extends SingleShortReader[Pressure] {
    override def read(ab: Int): Result[Pressure] = Ok(Pressure(ab - 32767))
}

object AbsoluteEvapSystemVaporPressureReader extends SingleShortReader[Pressure] {
    override def read(ab: Int): Result[Pressure] = Ok(Pressure.fromKiloPa(ab / 200.0))
}

case class InjectionTiming(timing: Double) extends Response {
    override def values(): Map[String, Value] = Map("timing" -> FloatValue(timing))
}

object FuelInjectionTimingReader extends SingleShortReader[InjectionTiming] {
    override def read(ab: Int): Result[InjectionTiming] = Ok(InjectionTiming((ab/128.0) - 210))
}

case class FuelTrim(trim: Double) extends Response {
    override def values(): Map[String, Value] = Map("trim" -> FloatValue(trim))
}

object FuelTrimReader extends SingleByteReader[FuelTrim] {
    override def read(a: Int): Result[FuelTrim] = Ok(FuelTrim((100.0/128.0) * a - 100))
}

case class AirFlowRate(rate: Double) extends Response {
    override def values(): Map[String, Value] = Map("rate" -> FloatValue(rate))
}

object AirFlowRateReader extends SingleShortReader[AirFlowRate] {
    override def read(ab: Int): Result[AirFlowRate] = Ok(AirFlowRate(ab / 100.0))
}

case class Count(count: Int) extends Response {
    override def values(): Map[String, Value] = Map("count" -> IntegerValue(count))
}

object CountReader extends SingleByteReader[Count] {
    override def read(a: Int): Result[Count] = Ok(Count(a))
}

case class OxygenSensorMaxValues(fuelAirEquiv: Double, voltage: Double, current: Double, pressure: Double) extends Response {
    override def values(): Map[String, Value] = Map(
        "fuel_air_equivalence" -> FloatValue(fuelAirEquiv),
        "voltage"              -> FloatValue(voltage),
        "current"              -> FloatValue(current),
        "pressure"             -> FloatValue(pressure)
    )
}

object OxygenSensorMaxValuesReader extends FourByteReader[OxygenSensorMaxValues] {
    override def read(a: Int, b: Int, c: Int, d: Int): Result[OxygenSensorMaxValues] =
        Ok(OxygenSensorMaxValues(a, b, Current.mA2A(c), Pressure.kPa2Pa(d * 10)))
}

case class MaximumValue(max: Double) extends Response {
    override def values(): Map[String, Value] = Map("max" -> FloatValue(max))
}

object MaximumAirFlowRateReader extends FourByteReader[MaximumValue] {
    override def read(a: Int, b: Int, c: Int, d: Int): Result[MaximumValue] = Ok(MaximumValue(a * 10))
}

case class Torque(torque: Double) extends Response {
    override def values(): Map[String, Value] = Map("torque" -> FloatValue(torque))
}

object TorqueReader extends SingleShortReader[Torque] {
    override def read(ab: Int): Result[Torque] = Ok(Torque(ab))
}

case class EngineTorqueData(idle: Double, point1: Double, point2: Double, point3: Double, point4: Double) extends Response {
    override def values(): Map[String, Value] = Map(
        "idle"   -> FloatValue(idle),
        "point1" -> FloatValue(point1),
        "point2" -> FloatValue(point2),
        "point3" -> FloatValue(point3),
        "point4" -> FloatValue(point4)
    )
}

object EngineTorqueDataReader extends FiveByteReader[EngineTorqueData] {
    override def read(a: Int, b: Int, c: Int, d: Int, e: Int): Result[EngineTorqueData] =
        Ok(EngineTorqueData(a / 255.0, b / 255.0, c / 255.0, d / 255.0, e / 255.0))
}
