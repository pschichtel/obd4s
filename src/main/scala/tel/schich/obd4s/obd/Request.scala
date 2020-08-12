package tel.schich.obd4s.obd

import enumeratum.EnumEntry

abstract class Request[T, S <: Service](val service: S, val reader: Reader[T]) extends EnumEntry {
    lazy val name: String = getClass.getSimpleName.replace("$", "")
}

abstract class ServiceRequest[T, S <: Service](service: S, reader: Reader[T]) extends Request[T, S](service, reader)

abstract class ParameterRequest[T, S <: Service](service: S, val pid: Int, reader: Reader[T]) extends Request[T, S](service, reader) {

    def isSupported(checkSupport: Int => Boolean): Boolean = checkSupport(pid)
    def bytes: Array[Byte]

    lazy val plain: PlainRequest[T] = PlainRequest(pid, reader)
}


