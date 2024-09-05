package example

import zio.*
import zio.test.*

class SecretWeapon(authenticator: Authenticator):

  def destroyUniverse(userId: String): UIO[String] =
    authenticator
      .authenticate(userId)
      .map { token =>
        s"Authentication succeeded with $token.\nPlanet destroyed! :)"
      }
      .catchAll {
        case AppError.UserNotFound =>
          ZIO.succeed(s"The user $userId does not exist")
        case AppError.UserNotAuthorized =>
          ZIO.succeed(s"The user $userId is not authorized to use this weapon")
      }

object SecretWeapon:

  val layer = ZLayer.derive[SecretWeapon]

  def destroyUniverse(userId: String) =
    ZIO.serviceWithZIO[SecretWeapon](_.destroyUniverse(userId))
