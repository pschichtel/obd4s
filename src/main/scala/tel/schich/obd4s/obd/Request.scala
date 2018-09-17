package tel.schich.obd4s.obd

import enumeratum.EnumEntry

abstract class Request[T, M <: Mode](val mode: M, val pid: Int, val reader: Reader[T]) extends EnumEntry {

    def isSupported(checkSupport: Int => Boolean): Boolean = checkSupport(pid)
    def bytes: Array[Byte]

    lazy val name: String = getClass.getSimpleName.replace("$", "")
    lazy val plain: PlainRequest[T] = PlainRequest(pid, reader)
}


