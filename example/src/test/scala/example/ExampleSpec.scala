package example

import zio.*
import zio.test.*

object ExampleSpec extends ZIOSpecDefault:
  val spec = suiteAll("ExampleSpec") {
    test("destroyUniverse") {
      for
        result <- SecretWeapon.destroyUniverse("123")
        token   = Token("b2c8ccb8-191a-4233-9b34-3e3111a4adaf")
      yield assertTrue(result == s"Authentication succeeded with $token.\nPlanet destroyed! :)")
    }
  }.provide(
    SecretWeapon.layer,
    Authenticator.live
  )
