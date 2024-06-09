package stubby
import zio.*
import scala.quoted.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

case class MethodId(name: String)

class ServicePartiallyApplied[Service]:
  inline def apply[Output](inline select: Service => Output)(inline result: Output): URIO[Stubbed[Service], Unit] =
    ${ Macros.stubImpl('select, 'result) }

def any[A]: A = ???

inline def stub[Service]                                        = ServicePartiallyApplied[Service]()
inline def stubbed[Service]: ULayer[Service & Stubbed[Service]] = ${ Macros.stubbedImpl[Service] }

enum StubbyError extends Exception:
  case NoStubFound(methodId: MethodId)

  override def getMessage(): String = this match
    case StubbyError.NoStubFound(methodId) => s"No stub found for ${methodId.name}"

trait Stubbed[A]:
  protected val $ref: ConcurrentHashMap[MethodId, Any] = new ConcurrentHashMap()

  protected def insert(methodId: MethodId, response: Any): UIO[Unit] =
    ZIO.succeed($ref.put(methodId, response))

  def callStubbed[A](methodId: MethodId): A =
    val response = $ref.getOrDefault(methodId, ZIO.fail(StubbyError.NoStubFound(methodId)))
    response.asInstanceOf[A]

object Stubbed:
  def insert[Service: Tag](
      methodId: MethodId,
      response: Any
  ): URIO[Stubbed[Service], Unit] =
    ZIO.serviceWithZIO(_.insert(methodId, response))

object Macros:

  def stubImpl[Service: Type, Output: Type](
      select: Expr[Service => Output],
      result: Expr[Output]
  )(using Quotes): Expr[URIO[Stubbed[Service], Unit]] =
    import quotes.reflect.*

    select.asTerm.underlyingArgument match
      case Lambda(args, body @ Apply(Select(_, methodName), _)) =>
        '{
          Stubbed.insert[Service](MethodId(${ Expr(methodName) }), $result)
        }

  def stubbedImpl[Service: Type](using Quotes): Expr[ULayer[Service & Stubbed[Service]]] =
    import quotes.reflect.*

    val serviceMethods = TypeRepr.of[Service].typeSymbol.declaredMethods

    val serviceRepr = TypeRepr.of[Service]

    val stubbedTypeRepr = TypeRepr.of[Stubbed[Service]]

    def decls(cls: Symbol): List[Symbol] = serviceMethods
      .map { method =>
        Symbol.newMethod(cls, method.name, serviceRepr.memberType(method))
      }

    val cls =
      experimental.newClassSym(
        Symbol.spliceOwner,
        s"${serviceRepr.typeSymbol.name}Stubbed",
        List(TypeRepr.of[Object], TypeRepr.of[Service], TypeRepr.of[Stubbed[Service]]),
        decls,
        selfType = None
      )

    val thisExpr = This(cls).asExprOf[Service & Stubbed[Service]]
    def body(refExpr: Expr[zio.Ref[Map[MethodId, Any]]]) = cls.declaredMethods.map { method =>
      val methodName = method.name
      val returnType = method.termRef.returnType

      DefDef(
        method,
        argss =>
          Some {
            '{
              $thisExpr.callStubbed(MethodId(${ Expr(methodName) }))
            }.asTerm
          }
      )
    }

    def clsDef(refExpr: Expr[zio.Ref[Map[MethodId, Any]]]) =
      experimental.ClassDef(
        cls,
        List(TypeTree.of[Object], TypeTree.of[Service], TypeTree.of[Stubbed[Service]]),
        body(refExpr)
      )

    val newCls = Typed(
      Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil),
      TypeTree.of[Service & Stubbed[Service]]
    )

    def makeClass(refExpr: Expr[zio.Ref[Map[MethodId, Any]]]) =
      Block(List(clsDef(refExpr)), newCls).asExprOf[Service & Stubbed[Service]]

    val result = '{
      ZLayer.fromZIOEnvironment {
        for
          ref    <- zio.Ref.make(Map.empty[MethodId, Any])
          stubbed = ${ makeClass('ref) }
        yield ZEnvironment[Service, Stubbed[Service]](stubbed, stubbed)
      }
    }

    result.asInstanceOf[Expr[ULayer[Service & Stubbed[Service]]]]

extension (using Quotes)(repr: quotes.reflect.TypeRepr)
  def returnType: quotes.reflect.TypeRepr =
    import quotes.reflect.*
    repr.widenTermRefByName match
      case MethodType(_, _, ret) => ret
      case _                     => repr
