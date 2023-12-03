package tel.schich.obd4s

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
    override def filter(p: T => Boolean): Result[T] = if (p(result)) Ok(result) else Error(InternalCause.FilteredAway)
    override def getOrElse[A >: T](alt: => A): A = result
}

final case class Error[T](cause: Cause) extends Result[T] {
    override def toEither: Either[Cause, T] = Left(cause)
    override def toOption: Option[T] = None
    override def map[A](f: T => A): Result[A] = Error(cause)
    override def flatMap[A](f: T => Result[A]): Result[A] = Error(cause)
    override def foreach[U](f: T => U): Unit = {}
    override def filter(p: T => Boolean): Result[T] = Error(cause)
    override def getOrElse[A >: T](alt: => A): A = alt
}
