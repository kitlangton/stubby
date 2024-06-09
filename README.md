# `stubby`

[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.kitlangton/stubby_3.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/io.github.kitlangton/stubby_3.svg "Sonatype Snapshots"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/kitlangton/stubby_3/ "Sonatype Snapshots"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/io/github/kitlangton/stubby_3/ "Sonatype Releases"

A simple stubbing library for ZIO tests.

```scala
libraryDependencies += "io.github.kitlangton" %% "stubby" % "0.1.0"
```

### Example Usage

Given the following contrived API.

```scala
trait MagicAPI:
  def destroyPlanet(planet: String): IO[MagicError, Unit]
  def pureMethod(int: Int): String

enum MagicError extends Exception:
  case MagicBackfired
  case TooManyMagic
```

And the following application logic.

```scala
class App(magicApi: MagicAPI):
  def destroy: Task[String] =
    magicApi
      .destroyPlanet("Earth")
      .as("Planet destroyed")
      .catchAll {
        case MagicError.MagicBackfired => ZIO.succeed("Magic backfired")
        case MagicError.TooManyMagic   => ZIO.succeed("Too many magic")
      }

  def usePure: String =
    s"Pure Magic: ${magicApi.pureMethod(42)}"

object App:
  val layer: ZLayer[MagicAPI, Nothing, App] =
    ZLayer.fromFunction(App.apply)

  def destroy = ZIO.serviceWithZIO[App](_.destroy)
  def usePure = ZIO.serviceWith[App](_.usePure)
```

Use `stubby` to stub methods and test your application:

```scala
object AppSpec extends ZIOSpecDefault:
  val spec =
    suiteAll("App") {

      test("fails when magic backfires") {
        for
          // Override the destroyPlanet method to always fail with MagicBackfired
          _ <- stub[MagicAPI](_.destroyPlanet(any)) {
                  ZIO.fail(MagicError.MagicBackfired)
                }
          result <- App.destroy
        yield assertTrue(result == "Magic backfired")
      }

      test("returns the result of the pure method") {
        for
          // Or stub a pure method to return a specific value
          _ <- stub[MagicAPI](_.pureMethod(any)) {
                  "HELP ME I'M TRAPPED IN AN EXAMPLE"
                }
          result <- App.usePure
        yield assertTrue(result == "Pure Magic: HELP ME I'M TRAPPED IN AN EXAMPLE")
      }

    }.provide(
      App.layer,
      stubbed[MagicAPI] // <- stubbed[MagicAPI] will create a Layer[MagicAPI]
    )
```
