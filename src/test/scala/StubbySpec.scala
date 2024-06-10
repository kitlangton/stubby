import zio.test.*
import zio.*

import stubby.*
import scala.compiletime.testing.typeCheckErrors

case class User(id: Int, name: String)

trait ExampleService:
  def effectWithParams(id: Int): Task[User]
  def effectWithoutParams: Task[Int]
  def pureWithParams(id: Int): Int
  def pureWithoutParams: Int

  def overloadedEffect(id: Int): Task[User]
  def overloadedEffect(name: String): Task[User]

object ExampleService:
  def effectWithParams(id: Int): ZIO[ExampleService, Throwable, User] =
    ZIO.serviceWithZIO[ExampleService](_.effectWithParams(id))
  def effectWithoutParams: ZIO[ExampleService, Throwable, Int] =
    ZIO.serviceWithZIO[ExampleService](_.effectWithoutParams)
  def pureWithParams(id: Int): ZIO[ExampleService, Nothing, Int] =
    ZIO.serviceWith[ExampleService](_.pureWithParams(id))
  def pureWithoutParams: ZIO[ExampleService, Nothing, Int] =
    ZIO.serviceWith[ExampleService](_.pureWithoutParams)
  def overloadedEffect(id: Int): ZIO[ExampleService, Throwable, User] =
    ZIO.serviceWithZIO[ExampleService](_.overloadedEffect(id))
  def overloadedEffect(name: String): ZIO[ExampleService, Throwable, User] =
    ZIO.serviceWithZIO[ExampleService](_.overloadedEffect(name))

object StubbySpec extends ZIOSpecDefault:
  val spec =
    suiteAll("StubbySpec") {

      suiteAll("stub") {

        suiteAll("effectful method with params") {

          test("succeeds when leaving off the params") {
            for
              _ <- stub[ExampleService](_.effectWithParams) {
                     ZIO.succeed(User(1, "John Doe"))
                   }
              result <- ExampleService.effectWithParams(1)
            yield assertTrue(result == User(1, "John Doe"))
          }

          test("succeeds when providing any params") {
            for
              _ <- stub[ExampleService](_.effectWithParams(any)) {
                     ZIO.succeed(User(1, "John Doe"))
                   }
              result <- ExampleService.effectWithParams(1)
            yield assertTrue(result == User(1, "John Doe"))
          }

          // test("succeeds when providing a handler") {
          //   for
          //     _ <- stub[ExampleService](_.effectWithParams) { arg =>
          //            ZIO.succeed(User(arg, "John Doe"))
          //          }
          //     result <- ExampleService.effectWithParams(1)
          //   yield assertTrue(result == User(1, "John Doe"))
          // }
        }

        suiteAll("effectful method without params") {

          test("succeeds when calling") {
            for
              _ <- stub[ExampleService](_.effectWithoutParams) {
                     ZIO.succeed(12)
                   }
              result <- ExampleService.effectWithoutParams
            yield assertTrue(result == 12)
          }

        }

        suiteAll("overloaded method") {

          test("succeeds when calling with id") {
            for
              _ <- stub[ExampleService](_.overloadedEffect(any[Int])) {
                     ZIO.succeed(User(1, "John Doe"))
                   }
              result <- ExampleService.overloadedEffect(1)
            yield assertTrue(result == User(1, "John Doe"))
          }

          test("succeeds when calling with name") {
            for
              _ <- stub[ExampleService](_.overloadedEffect(any[String])) {
                     ZIO.succeed(User(1, "John Doe"))
                   }
              result <- ExampleService.overloadedEffect("John Doe")
            yield assertTrue(result == User(1, "John Doe"))
          }

        }

      }

      suiteAll("error messages") {

        test("providing the wrong type") {
          val errors = typeCheckErrors {
            """
            stub[ExampleService](_.effectWithParams) {
              12
            }
            """
          }

          assertTrue(errors.head.message == "Expected zio.Task[User] but got scala.Int")
        }

      }

    }.provide(
      stubbed[ExampleService]
    )
