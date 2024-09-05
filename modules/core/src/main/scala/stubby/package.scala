package stubby
import zio.*
import scala.quoted.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

class StubPartiallyApplied[Service]:

  inline def apply[Output, Result](inline select: Service => Output)(
      inline result: Result
  ): URIO[Stubbed[Service], Unit] =
    ${ Macros.stubImpl('select, 'result) }

def any[A]: A = ???

inline def stub[Service]                                        = StubPartiallyApplied[Service]()
inline def stubbed[Service]: ULayer[Service & Stubbed[Service]] = ${ Macros.stubbedImpl[Service] }

extension (using Quotes)(repr: quotes.reflect.TypeRepr)
  def returnType: quotes.reflect.TypeRepr =
    import quotes.reflect.*
    repr.widenTermRefByName match
      case MethodType(_, _, ret) => ret
      case _                     => repr
