package db;

import doobie.Meta
import java.security.MessageDigest
import scala.collection.immutable.ArraySeq

type ByteSeq = ArraySeq[Byte]

implicit val byteSeqMeta: Meta[ByteSeq] =
  Meta[Array[Byte]].imap(ArraySeq.unsafeWrapArray)(_.toArray)

extension (array: ByteSeq)
  def digest: ByteSeq =
    ArraySeq.unsafeWrapArray(
      MessageDigest
        .getInstance("SHA-256")
        .digest(array.toArray)
    )

  def invert: ByteSeq =
    array.map(v => (~v).toByte)