package stubby

import zio.*
import scala.quoted.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

// I hope this works out.
case class MethodId(name: String)

enum StubResult:
  case Value[A](value: A)
  case F1[A, B](f: A => B)
  case F2[A, B, C](f: (A, B) => C)
  case F3[A, B, C, D](f: (A, B, C) => D)
  case F4[A, B, C, D, E](f: (A, B, C, D) => E)
  case F5[A, B, C, D, E, F](f: (A, B, C, D, E) => F)
  case F6[A, B, C, D, E, F, G](f: (A, B, C, D, E, F) => G)
  case F7[A, B, C, D, E, F, G, H](f: (A, B, C, D, E, F, G) => H)
  case F8[A, B, C, D, E, F, G, H, I](f: (A, B, C, D, E, F, G, H) => I)
  case F9[A, B, C, D, E, F, G, H, I, J](f: (A, B, C, D, E, F, G, H, I) => J)

  def call(args: Array[Any]): Any =
    this match
      case Value(value) => value

      case F1(f: (Any => Any) @unchecked) =>
        f(args(0))
      case F2(f: ((Any, Any) => Any) @unchecked) =>
        f(args(0), args(1))
      case F3(f: ((Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2))
      case F4(f: ((Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3))
      case F5(f: ((Any, Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3), args(4))
      case F6(f: ((Any, Any, Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3), args(4), args(5))
      case F7(f: ((Any, Any, Any, Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3), args(4), args(5), args(6))
      case F8(f: ((Any, Any, Any, Any, Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7))
      case F9(f: ((Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any) @unchecked) =>
        f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8))
      case _ => throw new IllegalArgumentException("Invalid stub result")

object StubResult:
  def makeF[A, B](f: A => B): StubResult                                                   = StubResult.F1(f)
  def makeF[A, B, C](f: (A, B) => C): StubResult                                           = StubResult.F2(f)
  def makeF[A, B, C, D](f: (A, B, C) => D): StubResult                                     = StubResult.F3(f)
  def makeF[A, B, C, D, E](f: (A, B, C, D) => E): StubResult                               = StubResult.F4(f)
  def makeF[A, B, C, D, E, F](f: (A, B, C, D, E) => F): StubResult                         = StubResult.F5(f)
  def makeF[A, B, C, D, E, F, G](f: (A, B, C, D, E, F) => G): StubResult                   = StubResult.F6(f)
  def makeF[A, B, C, D, E, F, G, H](f: (A, B, C, D, E, F, G) => H): StubResult             = StubResult.F7(f)
  def makeF[A, B, C, D, E, F, G, H, I](f: (A, B, C, D, E, F, G, H) => I): StubResult       = StubResult.F8(f)
  def makeF[A, B, C, D, E, F, G, H, I, J](f: (A, B, C, D, E, F, G, H, I) => J): StubResult = StubResult.F9(f)

trait Stubbed[A]:
  protected val $ref: ConcurrentHashMap[MethodId, StubResult] = new ConcurrentHashMap()

  protected def insertValue(methodId: MethodId, response: Any): UIO[Unit] =
    ZIO.succeed($ref.put(methodId, StubResult.Value(response)))

  protected def insertFunction(methodId: MethodId, f: StubResult): UIO[Unit] =
    ZIO.succeed($ref.put(methodId, f))

  def callStubbed[A](methodId: MethodId, args: Array[Any]): A =
    val response = $ref.getOrDefault(methodId, null)
    if response == null then throw new IllegalArgumentException("No stub found for methodId: " + methodId)
    else response.call(args).asInstanceOf[A]

object Stubbed:
  def insertValue[Service: Tag, Value](
      methodId: MethodId,
      response: Value
  ): URIO[Stubbed[Service], Unit] =
    ZIO.serviceWithZIO(_.insertValue(methodId, response))

  def insertFunction[Service: Tag](
      methodId: MethodId,
      f: StubResult
  ): URIO[Stubbed[Service], Unit] =
    ZIO.serviceWithZIO(_.insertFunction(methodId, f))
