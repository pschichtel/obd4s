package tel.schich.obd4s.obd

case class Sample(instant: Long, mode: Service, name: String, values: Map[String, Value])
