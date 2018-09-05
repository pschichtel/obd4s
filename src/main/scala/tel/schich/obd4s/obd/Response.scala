package tel.schich.obd4s.obd

trait Response {
    def values(): Map[String, Value]
}
