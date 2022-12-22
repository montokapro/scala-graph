package db;

import java.security.MessageDigest

// TODO: confirm digest has consistent length
def digest(value: String): String =
  MessageDigest
    .getInstance("SHA-256")
    .digest(value.getBytes("UTF-8"))
    .map("%02x".format(_))
    .mkString

extension (array: Array[Byte])
  def digest: Array[Byte] =
    MessageDigest
      .getInstance("SHA-256")
      .digest(array)

  def invert: Array[Byte] =
    array.map(v => (~v).toByte)

// Adapted from:
// https://github.com/scala/scala/blob/v2.13.10/src/library/scala/math/Ordering.scala#L269
implicit class ArrayOrdering[T: Ordering] extends Ordering[Array[T]] {
  def compare(x: Array[T], y: Array[T]) = {
    val xe = x.iterator
    val ye = y.iterator

    while (xe.hasNext && ye.hasNext) {
      val res = Ordering[T].compare(xe.next(), ye.next())
      if (res != 0) return res
    }

    Ordering[Boolean].compare(xe.hasNext, ye.hasNext)
  }
}

implicit class MonoidArray[T: scala.reflect.ClassTag] extends cats.Monoid[Array[T]] {
  def empty: Array[T] = Array.empty[T]

  def combine(x: Array[T], y: Array[T]) = x.concat(y)
}