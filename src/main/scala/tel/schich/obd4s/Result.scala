package tel.schich.obd4s

sealed trait Result[T] {
    def toEither: Either[String, T]
    def toOption: Option[T]
    def flatMap[A](f: T => Result[A]): Result[A]
}

object Result {
    def apply[T](result: T): Result[T] = Ok(result)
}

case class Ok[T](result: T) extends Result[T] {
    override def toEither: Either[String, T] = Right(result)
    override def toOption: Option[T] = Some(result)
    override def flatMap[A](f: T => Result[A]): Result[A] = f(result)
}

case class Failure[T](reason: String) extends Result[T] {
    override def toEither: Either[String, T] = Left(reason)
    override def toOption: Option[T] = None
    override def flatMap[A](f: T => Result[A]): Result[A] = Failure(reason)
}
