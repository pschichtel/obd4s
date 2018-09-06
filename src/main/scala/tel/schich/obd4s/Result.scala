package tel.schich.obd4s

sealed trait Result[+T] { self =>
    def toEither: Either[String, T]
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
    override def toEither: Either[String, T] = Right(result)
    override def toOption: Option[T] = Some(result)
    override def map[A](f: T => A): Result[A] = Ok(f(result))
    override def flatMap[A](f: T => Result[A]): Result[A] = f(result)
    override def foreach[U](f: T => U): Unit = f(result)
    override def filter(p: T => Boolean): Result[T] = if (p(result)) Ok(result) else Error("Filter did not apply!")
}

final case class Error[T](reason: String) extends Result[T] {
    override def toEither: Either[String, T] = Left(reason)
    override def toOption: Option[T] = None
    override def map[A](f: T => A): Result[A] = Error(reason)
    override def flatMap[A](f: T => Result[A]): Result[A] = Error(reason)
    override def foreach[U](f: T => U): Unit = {}
    override def filter(p: T => Boolean): Result[T] = Error(reason)
}
