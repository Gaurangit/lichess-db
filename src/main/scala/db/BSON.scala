package lila.db

import ornicar.scalalib.Zero
import org.joda.time.DateTime
import reactivemongo.bson._

import dsl._

abstract class BSON[T]
    extends BSONHandler[Bdoc, T]
    with BSONDocumentReader[T]
    with BSONDocumentWriter[T] {

  val logMalformed = true

  import BSON._

  def reads(reader: Reader): T
  def writes(writer: Writer, obj: T): Bdoc

  def read(doc: Bdoc): T = if (logMalformed) try {
    reads(new Reader(doc))
  }
  catch {
    case e: Exception =>
      println(s"Can't read malformed doc ${debug(doc)}", e)
      throw e
  }
  else reads(new Reader(doc))

  def write(obj: T): Bdoc = writes(writer, obj)
}

object BSON extends Handlers {

  def toDocHandler[A](implicit handler: BSONHandler[BSONDocument, A]): BSONDocumentHandler[A] =
    new BSONDocumentReader[A] with BSONDocumentWriter[A] with BSONHandler[BSONDocument, A] {
      def read(doc: BSONDocument) = handler read doc
      def write(o: A) = handler write o
    }

  final class Reader(val doc: BSONDocument) {

    val map = {
      // mutable optimized implementation
      val b = collection.immutable.Map.newBuilder[String, BSONValue]
      for (tuple <- doc.stream if tuple.isSuccess) b += (tuple.get.name -> tuple.get.value)
      b.result
    }

    def get[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      reader.asInstanceOf[BSONReader[BSONValue, A]] read map(k)
    def getO[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): Option[A] =
      map get k flatMap reader.asInstanceOf[BSONReader[BSONValue, A]].readOpt
    def getD[A](k: String)(implicit zero: Zero[A], reader: BSONReader[_ <: BSONValue, A]): A =
      getO[A](k) getOrElse zero.zero
    def getD[A](k: String, default: => A)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      getO[A](k) getOrElse default
    def getsD[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, List[A]]) =
      getO[List[A]](k) getOrElse Nil

    def str(k: String) = get[String](k)
    def strO(k: String) = getO[String](k)
    def strD(k: String) = strO(k) getOrElse ""
    def int(k: String) = get[Int](k)
    def intO(k: String) = getO[Int](k)
    def intD(k: String) = intO(k) getOrElse 0
    def double(k: String) = get[Double](k)
    def doubleO(k: String) = getO[Double](k)
    def doubleD(k: String) = doubleO(k) getOrElse 0
    def bool(k: String) = get[Boolean](k)
    def boolO(k: String) = getO[Boolean](k)
    def boolD(k: String) = boolO(k) getOrElse false
    def date(k: String) = get[DateTime](k)
    def dateO(k: String) = getO[DateTime](k)
    def dateD(k: String, default: => DateTime) = getD(k, default)
    def bytes(k: String) = get[ByteArray](k)
    def bytesO(k: String) = getO[ByteArray](k)
    def bytesD(k: String) = bytesO(k) getOrElse ByteArray.empty
    def nInt(k: String) = get[BSONNumberLike](k).toInt
    def nIntO(k: String) = getO[BSONNumberLike](k) map (_.toInt)
    def nIntD(k: String) = nIntO(k) getOrElse 0
    def intsD(k: String) = getO[List[Int]](k) getOrElse Nil
    def strsD(k: String) = getO[List[String]](k) getOrElse Nil

    def toList = doc.elements.toList

    def contains = map contains _

    def debug = BSON debug doc
  }

  final class Writer {

    def boolO(b: Boolean): Option[BSONBoolean] = if (b) Some(BSONBoolean(true)) else None
    def str(s: String): BSONString = BSONString(s)
    def strO(s: String): Option[BSONString] = if (s.nonEmpty) Some(BSONString(s)) else None
    def int(i: Int): BSONInteger = BSONInteger(i)
    def intO(i: Int): Option[BSONInteger] = if (i != 0) Some(BSONInteger(i)) else None
    def date(d: DateTime): BSONDateTime = BSONJodaDateTimeHandler write d
    def byteArrayO(b: ByteArray): Option[BSONBinary] =
      if (b.isEmpty) None else Some(ByteArray.ByteArrayBSONHandler.write(b))
    def bytesO(b: Array[Byte]): Option[BSONBinary] = byteArrayO(ByteArray(b))
    def strListO(list: List[String]): Option[List[String]] = list match {
      case Nil => None
      case List("") => None
      case List("", "") => None
      case List(a, "") => Some(List(a))
      case full => Some(full)
    }
    def listO[A](list: List[A])(implicit writer: BSONWriter[A, _ <: BSONValue]): Option[Barr] =
      if (list.isEmpty) None
      else Some(BSONArray(list map writer.write))
    def docO(o: Bdoc): Option[Bdoc] = if (o.isEmpty) None else Some(o)
    def double(i: Double): BSONDouble = BSONDouble(i)
    def doubleO(i: Double): Option[BSONDouble] = if (i != 0) Some(BSONDouble(i)) else None
  }

  val writer = new Writer

  def debug(v: BSONValue): String = v match {
    case d: Bdoc => debugDoc(d)
    case d: Barr => debugArr(d)
    case BSONString(x) => x
    case BSONInteger(x) => x.toString
    case BSONDouble(x) => x.toString
    case BSONBoolean(x) => x.toString
    case v => v.toString
  }
  def debugArr(doc: Barr): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: Bdoc): String = (doc.elements.toList map {
    case BSONElement(k, v) => s"$k: ${debug(v)}"
  }).mkString("{", ", ", "}")

  def hashDoc(doc: Bdoc): String = debugDoc(doc).replace(" ", "")
}
