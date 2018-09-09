package tel.schich.obd4s

import enumeratum.{Enum, EnumEntry}

sealed trait Result[+T] { self =>
    def toEither: Either[Cause, T]
    def toOption: Option[T]
    def map[A](f: T => A): Result[A]
    def flatMap[A](f: T => Result[A]): Result[A]
    def foreach[U](f: T => U): Unit
    def filter(p: T => Boolean): Result[T]
    def withFilter(p: T => Boolean): WithFilter = new WithFilter(p)
    def getOrElse[A >: T](alt: => A): A

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
    override def filter(p: T => Boolean): Result[T] = if (p(result)) Ok(result) else Error(InternalCauses.FilteredAway)
    override def getOrElse[A >: T](alt: => A): A = result
}

sealed case class Error[T](cause: Cause) extends Result[T] {
    override def toEither: Either[Cause, T] = Left(cause)
    override def toOption: Option[T] = None
    override def map[A](f: T => A): Result[A] = Error(cause)
    override def flatMap[A](f: T => Result[A]): Result[A] = Error(cause)
    override def foreach[U](f: T => U): Unit = {}
    override def filter(p: T => Boolean): Result[T] = Error(cause)
    override def getOrElse[A >: T](alt: => A): A = alt
}

sealed case class Cause(code: Int, reason: String) extends EnumEntry

trait Causes { self: Enum[Cause] =>
    lazy val lookupByCode: Map[Int, Cause] =
        values.map(c => (c.code, c)).toMap
}

object InternalCauses extends Enum[Cause] with Causes {
    object UnknownCause extends Cause(0, "Unknown error!")
    object FilteredAway extends Cause(1, "Filter did not apply!")
    object ResponseTooShort extends Cause(2, "Filter did not apply!")
    object WrongSid extends Cause(3, "Successful response, but for the wrong SID!")
    object UnknownResponse extends Cause(4, "ECU returned an unknown response!")
    object Timeout extends Cause(5, "Request timed out!")
    object PidMismatch extends Cause(6, "A response PID did not match the request PID!")

    override val values = findValues
}

object ObdCauses extends Enum[Cause] with Causes {

    private val ServiceOrSubfunctionNotSupported = "Service or Subfunction not supported (in active Session)"

    object GeneralReject extends Cause(0x10, "General reject")
    object ServiceOrSubfunctionNotSupportedA extends Cause(0x11, ServiceOrSubfunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedB extends Cause(0x12, ServiceOrSubfunctionNotSupported)
    object MessageLengthOrFormatIncorrect extends Cause(0x13, "Message length or format incorrect")
    object BusyRepeatRequest extends Cause(0x21, "Busy - Repeat request")
    object ConditionsNotCorrect extends Cause(0x22, "Conditions not correct")
    object RequestSequenceError extends Cause(0x24, "Request sequence error")
    object OutOfRange extends Cause(0x31, "Out of range")
    object SecurityAccessDenied extends Cause(0x33, "Security access denied")
    object InvalidKey extends Cause(0x35, "Invalid key")
    object ExceedAttempts extends Cause(0x36, "Exceed attempts")
    object BusyResponsePending extends Cause(0x78, "Busy - Response pending")
    object ServiceOrSubfunctionNotSupportedC extends Cause(0x7E, ServiceOrSubfunctionNotSupported)
    object ServiceOrSubfunctionNotSupportedD extends Cause(0x7F, ServiceOrSubfunctionNotSupported)

    override val values = findValues
}

object ElmCauses extends Enum[Cause] with Causes {
    object NoData extends Cause(1, "No data returned for request")
    object NoResponse extends Cause(2, "No response received")
    object UnknownOrInvalidCommand extends Cause(3, "Unknown or invalid command")

    override val values = findValues
}

