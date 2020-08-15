package tel.schich.obd4s.obd

import scala.annotation.tailrec

sealed trait Value
case class EnumarableValue(value: Value) extends Value
case class IntegerValue(value: Long) extends Value
case class FloatValue(value: Double) extends Value
case class StringValue(value: String) extends Value
case class TruthValue(value: Boolean) extends Value

object Value {
    @tailrec
    def stringify(v: Value): String = {
        v match {
            case IntegerValue(n)    => s"$n"
            case FloatValue(n)      => s"$n"
            case TruthValue(true)   => "t"
            case TruthValue(false)  => "f"
            case StringValue(s)     => s
            case EnumarableValue(w) => stringify(w)
        }
    }
}
