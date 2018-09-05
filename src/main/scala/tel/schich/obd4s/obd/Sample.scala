package tel.schich.obd4s.obd

case class Sample(instant: Long, mode: Mode, name: String, values: Map[String, Value])
