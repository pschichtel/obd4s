package tel.schich.obd4s.obd

case class PlainRequest[T](pid: Int, reader: Reader[T])
