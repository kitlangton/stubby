import zio.test.*
import zio.*
import stubby.*

trait MagicAPI:
  def destroyPlanet(planet: String): IO[MagicError, Unit]
  def rebuildPlanet(int: Int): IO[MagicError, Unit]
  def pureMethod(int: Int): String

enum MagicError extends Exception:
  case MagicBackfired
  case TooManyMagic

class App(magicApi: MagicAPI):
  def destroy: Task[String] =
    magicApi
      .destroyPlanet("Earth")
      .as("Planet destroyed")
      .catchAll {
        case MagicError.MagicBackfired => ZIO.succeed("Magic backfired")
        case MagicError.TooManyMagic   => ZIO.succeed("Too many magic")
      }

  def usePure: String = s"Pure Magic: ${magicApi.pureMethod(42)}"

object App:

  val layer: ZLayer[MagicAPI, Nothing, App] = ZLayer.fromFunction(App.apply)

  def destroy = ZIO.serviceWithZIO[App](_.destroy)
  def usePure = ZIO.serviceWith[App](_.usePure)

object AppSpec extends ZIOSpecDefault:

  val spec =
    suiteAll("App") {

      suiteAll("stub ZIO method") {

        test("fails when magic backfires") {
          for
            _ <- stub[MagicAPI](_.destroyPlanet(any)) {
                   ZIO.fail(MagicError.MagicBackfired)
                 }
            result <- App.destroy
          yield assertTrue(result == "Magic backfired")
        }

        test("fails when too many magic") {
          for
            _ <- stub[MagicAPI](_.destroyPlanet(any)) {
                   ZIO.fail(MagicError.TooManyMagic)
                 }
            result <- App.destroy
          yield assertTrue(result == "Too many magic")
        }

        test("successfully destroys planet") {
          for
            _ <- stub[MagicAPI](_.destroyPlanet(any)) {
                   ZIO.succeed(())
                 }
            result <- App.destroy
          yield assertTrue(result == "Planet destroyed")
        }

        test("counts calls") {
          for
            stubUsage <- stub[MagicAPI](_.destroyPlanet(any)) {
                           ZIO.succeed(())
                         }
            _ <- App.destroy
            _ <- App.destroy
            _ <- App.destroy
          yield assertTrue(stubUsage.calls == 3)
        }
      }

      suiteAll("stub pure method") {

        test("returns the result of the pure method") {
          for
            _ <- stub[MagicAPI](_.pureMethod(any)) {
                   "Hello"
                 }
            result <- App.usePure
          yield assertTrue(result == "Pure Magic: Hello")
        }

        test("counts calls") {
          for
            stubUsage <- stub[MagicAPI](_.pureMethod(any)) {
                           "Hello"
                         }
            _ <- App.usePure
            _ <- App.usePure
          yield assertTrue(stubUsage.calls == 2)
        }
      }

    }.provide(
      stubbed[MagicAPI],
      App.layer
    )
