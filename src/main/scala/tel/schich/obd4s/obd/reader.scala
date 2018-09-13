package tel.schich.obd4s.obd

import tel.schich.obd4s.{Error, InternalCauses, Result}

import scala.collection.SeqView

trait Reader[T] {
    def read(buf: IndexedSeq[Byte], offset: Int): Result[(T, Int)]
    // simple default strategy: select the last
    def merge(left: T, right: T): T = right
}

abstract class FixedLengthReader[T](val length: Int) extends Reader[T] {
    type BufferView = SeqView[Byte, IndexedSeq[Byte]]

    final override def read(buf: IndexedSeq[Byte], offset: Int): Result[(T, Int)] = {
        if ((buf.length - offset) < length) Error(InternalCauses.ResponseTooShort)
        else read(buf.view(offset, offset + length)).map(r => (r, length))
    }

    def read(buf: BufferView): Result[T]
}

abstract class SingleByteReader[T] extends FixedLengthReader[T](1) {
    final override def read(buf: BufferView): Result[T] = {
        read(buf.head & 0xFF)
    }

    def read(a: Int): Result[T]
}

abstract class TwoByteReader[T] extends FixedLengthReader[T](2) {
    final override def read(buf: BufferView): Result[T] = {
        read(buf.head & 0xFF, buf(1) & 0xFF)
    }

    def read(a: Int, b: Int): Result[T]
}

abstract class FourByteReader[T] extends FixedLengthReader[T](4) {
    final override def read(buf: BufferView): Result[T] = {
        read(buf.head & 0xFF, buf(1) & 0xFF, buf(2) & 0xFF, buf(3) & 0xFF)
    }

    def read(a: Int, b: Int, c: Int, d: Int): Result[T]
}

abstract class FiveByteReader[T] extends FixedLengthReader[T](5) {
    final override def read(buf: BufferView): Result[T] = {
        read(buf.head & 0xFF, buf(1) & 0xFF, buf(2) & 0xFF, buf(3) & 0xFF, buf(4) & 0xFF)
    }

    def read(a: Int, b: Int, c: Int, d: Int, e: Int): Result[T]
}

abstract class SingleShortReader[T] extends TwoByteReader[T] {
    final override def read(a: Int, b: Int): Result[T] = {
        read(256 * a + b)
    }

    def read(ab: Int): Result[T]
}

abstract class TwoShortReader[T] extends FourByteReader[T] {
    final override def read(a: Int, b: Int, c: Int, d: Int): Result[T] = {
        read(256 * a + b, 256 * c + d)
    }

    def read(ab: Int, cd: Int): Result[T]
}

abstract class SingleIntReader[T] extends FourByteReader[T] {
    final override def read(a: Int, b: Int, c: Int, d: Int): Result[T] = {
        read(a << 24 | b << 16 | c << 8 | d)
    }

    def read(abcd: Int): Result[T]
}
