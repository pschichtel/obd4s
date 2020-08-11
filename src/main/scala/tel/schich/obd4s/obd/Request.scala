package tel.schich.obd4s.obd

import enumeratum.EnumEntry

abstract class Request[T, M <: Service](val service: M, val reader: Reader[T]) extends EnumEntry {
    lazy val name: String = getClass.getSimpleName.replace("$", "")
}

abstract class ServiceRequest[T, M <: Service](service: M, reader: Reader[T]) extends Request[T, M](service, reader)

abstract class ParameterRequest[T, M <: Service](service: M, val pid: Int, reader: Reader[T]) extends Request[T, M](service, reader) {

    def isSupported(checkSupport: Int => Boolean): Boolean = checkSupport(pid)
    def bytes: Array[Byte]

    lazy val plain: PlainRequest[T] = PlainRequest(pid, reader)
}


