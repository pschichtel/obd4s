package tel.schich.obd4s

import enumeratum.{Enum, EnumEntry}
import tel.schich.obd4s.Causes.FilteredAway

sealed trait Result[+T] { self =>
    def toEither: Either[Cause, T]
    def toOption: Option[T]
    def map[A](f: T => A): Result[A]
    def flatMap[A](f: T => Result[A]): Result[A]
    def foreach[U](f: T => U): Unit
    def filter(p: T => Boolean): Result[T]
    def withFilter(p: T => Boolean): WithFilter = new WithFilter(p)

    /** We need a whole WithFilter class to honor the "doesn't create a new
      *  collection" contract even though it seems unlikely to matter much in a
      *  collection with max size 1.
      */
    class WithFilter(p: T => Boolean) {
        def map[B](f: T => B): Result[B] = self filter p map f
        def flatMap[B](f: T => Result[B]): Result[B] = self filter p flatMap f
        def foreach[U](f: T => U): Unit = self filter p foreach f
        def withFilter(q: T => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
    }
}

object Result {
    def apply[T](result: T): Result[T] = Ok(result)
}

final case class Ok[T](result: T) extends Result[T] {
    override def toEither: Either[Cause, T] = Right(result)
    override def toOption: Option[T] = Some(result)
    override def map[A](f: T => A): Result[A] = Ok(f(result))
    override def flatMap[A](f: T => Result[A]): Result[A] = f(result)
    override def foreach[U](f: T => U): Unit = f(result)
    override def filter(p: T => Boolean): Result[T] = if (p(result)) Ok(result) else Error(FilteredAway)
}

final case class Error[T](cause: Cause) extends Result[T] {
    override def toEither: Either[Cause, T] = Left(cause)
    override def toOption: Option[T] = None
    override def map[A](f: T => A): Result[A] = Error(cause)
    override def flatMap[A](f: T => Result[A]): Result[A] = Error(cause)
    override def foreach[U](f: T => U): Unit = {}
    override def filter(p: T => Boolean): Result[T] = Error(cause)
}

sealed abstract class Cause(val code: Int, val reason: String) extends EnumEntry
object Causes extends Enum[Cause] {

    private val ServiceOrSubfunctionNotSupported = "Service or Subfunction not supported (in active Session)"

    case object UnknownCause extends Cause(0, "Unknown error!")
    case object FilteredAway extends Cause(-1, "Filter did not apply!")
    case object ResponseTooShort extends Cause(-2, "Filter did not apply!")
    case object WrongSid extends Cause(-3, "Successful response, but for the wrong SID!")
    case object UnknownResponse extends Cause(-4, "ECU returned an unknown response!")

    case object GeneralReject extends Cause(0x10, "General reject")
    case object ServiceOrSubfunctionNotSupportedA extends Cause(0x11, ServiceOrSubfunctionNotSupported)
    case object ServiceOrSubfunctionNotSupportedB extends Cause(0x12, ServiceOrSubfunctionNotSupported)
    case object MessageLengthOrFormatIncorrect extends Cause(0x13, "Message length or format incorrect")
    case object BusyRepeatRequest extends Cause(0x21, "Busy - Repeat request")
    case object ConditionsNotCorrect extends Cause(0x22, "Conditions not correct")
    case object RequestSequenceError extends Cause(0x24, "Request sequence error")
    case object OutOfRange extends Cause(0x31, "Out of range")
    case object SecurityAccessDenied extends Cause(0x33, "Security access denied")
    case object InvalidKey extends Cause(0x35, "Invalid key")
    case object ExceedAttempts extends Cause(0x36, "Exceed attempts")
    case object BusyResponsePending extends Cause(0x78, "Busy - Response pending")
    case object ServiceOrSubfunctionNotSupportedC extends Cause(0x7E, ServiceOrSubfunctionNotSupported)
    case object ServiceOrSubfunctionNotSupportedD extends Cause(0x7F, ServiceOrSubfunctionNotSupported)

    override def values = findValues

    lazy val lookupByCode: Map[Int, Cause] =
        values.map(c => (c.code, c)).toMap
}

