package example

import zio.*
import zio.test.*

enum AppError:
  case UserNotFound
  case UserNotAuthorized

case class Token(value: String)

trait Authenticator:
  def authenticate(userId: String): IO[AppError, Token]

object Authenticator:
  val live = ZLayer.derive[AuthenticatorLive]

class AuthenticatorLive() extends Authenticator:
  def authenticate(userId: String): IO[AppError, Token] =
    for
      uuid <- Random.nextUUID.map(_.toString)
      _    <- ZIO.fail(AppError.UserNotAuthorized).when(uuid.contains("7"))
    yield Token(uuid.toString)
