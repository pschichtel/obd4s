package tel.schich.obd4s.obd

import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.TimeUnit.{MINUTES, SECONDS}

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.obd.StandardModes.{CurrentData, VehicleInfo}

sealed abstract class PredefinedRequest[T, M <: Mode](mode: M, pid: Int, reader: Reader[T]) extends Request(mode, pid, reader) with EnumEntry {
    override val bytes: Array[Byte] = Array(pid.toByte)
}

object CurrentDataRequests extends Enum[PredefinedRequest[_ <: Response, CurrentData.type]] {
    case object Support01To20                        extends PredefinedRequest(CurrentData, 0x00, PidSupportReader)
    case object FuelSystemStatus                     extends PredefinedRequest(CurrentData, 0x03, FuelSystemStatusReader)
    case object EngineLoad                           extends PredefinedRequest(CurrentData, 0x04, EngineLoadReader)
    case object CoolantTemperature                   extends PredefinedRequest(CurrentData, 0x05, TemperatureReader)
    case object ShortTermFuelTrim1                   extends PredefinedRequest(CurrentData, 0x06, FuelTrimReader)
    case object LongTermFuelTrim1                    extends PredefinedRequest(CurrentData, 0x07, FuelTrimReader)
    case object ShortTermFuelTrim2                   extends PredefinedRequest(CurrentData, 0x08, FuelTrimReader)
    case object LongTermFuelTrim2                    extends PredefinedRequest(CurrentData, 0x09, FuelTrimReader)
    case object FuelPressure                         extends PredefinedRequest(CurrentData, 0x0A, FuelPressureReader)
    case object IntakeManifoldAbsolutePressure       extends PredefinedRequest(CurrentData, 0x0B, IntakeManifoldPressureReader)
    case object EngineRpm                            extends PredefinedRequest(CurrentData, 0x0C, RpmReader)
    case object VehicleSpeed                         extends PredefinedRequest(CurrentData, 0x0D, VehicleSpeedReader)
    case object TimingAdvance                        extends PredefinedRequest(CurrentData, 0x0E, TimingAdvanceReader)
    case object IntakeAirTemperature                 extends PredefinedRequest(CurrentData, 0x0F, TemperatureReader)
    case object AirFlowRate                          extends PredefinedRequest(CurrentData, 0x10, AirFlowRateReader)
    case object ThrottlePosition                     extends PredefinedRequest(CurrentData, 0x11, SingleBytePercentageReader)
    case object OxygenSensorFuelVoltage1             extends PredefinedRequest(CurrentData, 0x14, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage2             extends PredefinedRequest(CurrentData, 0x15, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage3             extends PredefinedRequest(CurrentData, 0x16, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage4             extends PredefinedRequest(CurrentData, 0x17, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage5             extends PredefinedRequest(CurrentData, 0x18, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage6             extends PredefinedRequest(CurrentData, 0x19, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage7             extends PredefinedRequest(CurrentData, 0x1A, OxygenSensorFuelVoltageReader)
    case object OxygenSensorFuelVoltage8             extends PredefinedRequest(CurrentData, 0x1B, OxygenSensorFuelVoltageReader)
    case object EngineRuntime                        extends PredefinedRequest(CurrentData, 0x1F, RuntimeReader(SECONDS))
    case object Support21To40                        extends PredefinedRequest(CurrentData, 0x20, PidSupportReader)
    case object DistanceTraveledWithMilOn            extends PredefinedRequest(CurrentData, 0x21, DistanceReader(DistanceReader.KiloMeter))
    case object FuelRailPressureRelativeToManifold   extends PredefinedRequest(CurrentData, 0x22, FuelRailPressureRelativeToManifoldReader)
    case object FuelRailGaugePressure                extends PredefinedRequest(CurrentData, 0x23, FuelRailGaugePressureReader)
    case object OxygenSensor1FuelAirVoltage1         extends PredefinedRequest(CurrentData, 0x24, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage2         extends PredefinedRequest(CurrentData, 0x25, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage3         extends PredefinedRequest(CurrentData, 0x26, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage4         extends PredefinedRequest(CurrentData, 0x27, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage5         extends PredefinedRequest(CurrentData, 0x28, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage6         extends PredefinedRequest(CurrentData, 0x29, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage7         extends PredefinedRequest(CurrentData, 0x2A, OxygenSensorFuelAirVoltageReader)
    case object OxygenSensor1FuelAirVoltage8         extends PredefinedRequest(CurrentData, 0x2B, OxygenSensorFuelAirVoltageReader)
    case object CommandedExhaustGasRecirculation     extends PredefinedRequest(CurrentData, 0x2C, SingleBytePercentageReader)
    case object ExhaustGasRecirculationError         extends PredefinedRequest(CurrentData, 0x2D, SingleByteSignedPercentageReader)
    case object CommandedEvaporativePurge            extends PredefinedRequest(CurrentData, 0x2E, SingleBytePercentageReader)
    case object FuelTankLevelInput                   extends PredefinedRequest(CurrentData, 0x2F, SingleBytePercentageReader)
    case object WarmupsSinceCodesCleared             extends PredefinedRequest(CurrentData, 0x30, CountReader)
    case object DistanceTraveledSinceCodeClear       extends PredefinedRequest(CurrentData, 0x31, DistanceReader(DistanceReader.KiloMeter))
    case object SystemVaporPressure                  extends PredefinedRequest(CurrentData, 0x32, SystemVaporPressureReader)
    case object AbsoluteBarometricPressure           extends PredefinedRequest(CurrentData, 0x33, BarometricPressureReader)
    case object OxygenSensor1FuelAirCurrent1         extends PredefinedRequest(CurrentData, 0x34, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent2         extends PredefinedRequest(CurrentData, 0x35, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent3         extends PredefinedRequest(CurrentData, 0x36, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent4         extends PredefinedRequest(CurrentData, 0x37, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent5         extends PredefinedRequest(CurrentData, 0x38, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent6         extends PredefinedRequest(CurrentData, 0x39, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent7         extends PredefinedRequest(CurrentData, 0x3A, OxygenSensorFuelAirCurrentReader)
    case object OxygenSensor1FuelAirCurrent8         extends PredefinedRequest(CurrentData, 0x3B, OxygenSensorFuelAirCurrentReader)
    case object CatalystTemperatureBank1             extends PredefinedRequest(CurrentData, 0x3C, CatalystTemperatureReader)
    case object CatalystTemperatureBank2             extends PredefinedRequest(CurrentData, 0x3D, CatalystTemperatureReader)
    case object CatalystTemperatureBank3             extends PredefinedRequest(CurrentData, 0x3E, CatalystTemperatureReader)
    case object CatalystTemperatureBank4             extends PredefinedRequest(CurrentData, 0x3F, CatalystTemperatureReader)
    case object Support41To60                        extends PredefinedRequest(CurrentData, 0x40, PidSupportReader)
    case object ControlModuleVoltage                 extends PredefinedRequest(CurrentData, 0x42, ControlModuleVoltageReader)
    case object AbsoluteLoadValue                    extends PredefinedRequest(CurrentData, 0x43, SingleBytePercentageReader)
    case object FuelAirCommandedEquivalenceRatio     extends PredefinedRequest(CurrentData, 0x44, FuelAirEquivalenceRatioReader)
    case object RelativeThrottlePosition             extends PredefinedRequest(CurrentData, 0x45, SingleBytePercentageReader)
    case object AmbientAirTemperature                extends PredefinedRequest(CurrentData, 0x46, TemperatureReader)
    case object AbsoluteThrottlePositionB            extends PredefinedRequest(CurrentData, 0x47, SingleBytePercentageReader)
    case object AbsoluteThrottlePositionC            extends PredefinedRequest(CurrentData, 0x48, SingleBytePercentageReader)
    case object AcceleratorPedalPositionD            extends PredefinedRequest(CurrentData, 0x49, SingleBytePercentageReader)
    case object AcceleratorPedalPositionE            extends PredefinedRequest(CurrentData, 0x4A, SingleBytePercentageReader)
    case object AcceleratorPedalPositionF            extends PredefinedRequest(CurrentData, 0x4B, SingleBytePercentageReader)
    case object CommandedThrottleActuator            extends PredefinedRequest(CurrentData, 0x4C, SingleBytePercentageReader)
    case object TimeRunWithMilOn                     extends PredefinedRequest(CurrentData, 0x4D, RuntimeReader(MINUTES))
    case object TimeSinceTroubleCodesCleared         extends PredefinedRequest(CurrentData, 0x4E, RuntimeReader(MINUTES))
    case object OxygenSensorMaxValues                extends PredefinedRequest(CurrentData, 0x4F, OxygenSensorMaxValuesReader)
    case object MaximumAirFlowRate                   extends PredefinedRequest(CurrentData, 0x50, MaximumAirFlowRateReader)
    case object FuelType                             extends PredefinedRequest(CurrentData, 0x51, FuelTypeReader)
    case object EthanolFuelPercentage                extends PredefinedRequest(CurrentData, 0x52, SingleBytePercentageReader)
    case object AbsoluteEvapSystemVaporPressure      extends PredefinedRequest(CurrentData, 0x53, AbsoluteEvapSystemVaporPressureReader)
    case object EvapSystemVaporPressure              extends PredefinedRequest(CurrentData, 0x54, EvapSystemVaporPressureReader)
    case object ShortTermSecondaryOxygenSensorTrim13 extends PredefinedRequest(CurrentData, 0x55, SecondaryOxygenSensorTrimReader)
    case object LongTermSecondaryOxygenSensorTrim13  extends PredefinedRequest(CurrentData, 0x56, SecondaryOxygenSensorTrimReader)
    case object ShortTermSecondaryOxygenSensorTrim24 extends PredefinedRequest(CurrentData, 0x57, SecondaryOxygenSensorTrimReader)
    case object LongTermSecondaryOxygenSensorTrim24  extends PredefinedRequest(CurrentData, 0x58, SecondaryOxygenSensorTrimReader)
    case object FuelRailAbsolutePressure             extends PredefinedRequest(CurrentData, 0x59, FuelRailPressureReader)
    case object RelativeAcceleratorPedalPosition     extends PredefinedRequest(CurrentData, 0x5A, SingleBytePercentageReader)
    case object HybridBatteryPackRemainingLife       extends PredefinedRequest(CurrentData, 0x5B, SingleBytePercentageReader)
    case object EngineOilTemperature                 extends PredefinedRequest(CurrentData, 0x5C, TemperatureReader)
    case object FuelInjectionTiming                  extends PredefinedRequest(CurrentData, 0x5D, FuelInjectionTimingReader)
    case object EngineFuelRate                       extends PredefinedRequest(CurrentData, 0x5E, FuelRateReader)
    case object Support61To80                        extends PredefinedRequest(CurrentData, 0x60, PidSupportReader)
    case object DriverDemandTorque                   extends PredefinedRequest(CurrentData, 0x61, SingleBytePercentageReader)
    case object ActualEngineTorque                   extends PredefinedRequest(CurrentData, 0x62, SingleBytePercentageReader)
    case object EngineReferenceTorque                extends PredefinedRequest(CurrentData, 0x63, TorqueReader)
    case object EngineTorqueData                     extends PredefinedRequest(CurrentData, 0x64, EngineTorqueDataReader)
    case object Support81ToA0                        extends PredefinedRequest(CurrentData, 0x80, PidSupportReader)
    case object SupportA1ToC0                        extends PredefinedRequest(CurrentData, 0xA0, PidSupportReader)
    case object SupportC1ToE0                        extends PredefinedRequest(CurrentData, 0xC0, PidSupportReader)

    val values = findValues

    lazy val reverse: Map[Int, PredefinedRequest[_ <: Response, _ <: Mode]] = values.map(v => (v.pid, v)).toMap
}

object VehicleInfoRequests extends Enum[PredefinedRequest[_, VehicleInfo.type]] {
    case object Support01To20         extends PredefinedRequest(VehicleInfo, 0x00, PidSupportReader)
    case object VehicleIdMessageCount extends PredefinedRequest(VehicleInfo, 0x01, ByteReader)
    case object VehicleId             extends PredefinedRequest(VehicleInfo, 0x02, StringReader(US_ASCII))
    case object EcuNameMessageCount   extends PredefinedRequest(VehicleInfo, 0x09, ByteReader)
    case object EcuName               extends PredefinedRequest(VehicleInfo, 0x0A, StringReader(US_ASCII))

    override val values = findValues
}

object ClearTroubleCodeRequests extends Enum[PredefinedRequest[_ , VehicleInfo.type]] {

    override val values = findValues
}
