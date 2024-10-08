package stubby

import zio.*
import scala.quoted.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

enum StubbyError extends Exception:
  case NoStubFound(methodId: MethodId)

  override def getMessage(): String = this match
    case StubbyError.NoStubFound(methodId) => s"No stub found for ${methodId.name}"
