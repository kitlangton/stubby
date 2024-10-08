package stubby

import zio.*
import scala.quoted.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

object Macros:

  def getMethod[Service: Type, Output: Type](using Quotes)(select: Expr[Service => Output]): quotes.reflect.Symbol =
    import quotes.reflect.*

    select.asTerm.underlyingArgument match

      // def methodName(arg: Int): Result = ???
      // stub[Service](_.methodName(any))
      case Lambda(_, Apply(select @ Select(_, methodName), _)) =>
        select.symbol

      // def methodName: Result = ???
      // stub[Service](_.methodName)
      case Lambda(_, select @ Select(_, methodName)) =>
        select.symbol

      // def methodName(arg: Int): Result = ???
      // stub[Service](_.methodName)
      case Lambda(args, Lambda(_, Apply(select @ Select(_, methodName), _))) =>
        select.symbol

      case _ =>
        // TODO: better error message
        report.errorAndAbort(s"Invalid selector: ${select.show}")

  def stubImpl[Service: Type, Output: Type](
      select: Expr[Service => Output],
      result: Expr[Output]
  )(using Quotes): Expr[URIO[Stubbed[Service], StubUsage]] =
    import quotes.reflect.*

    val method = getMethod(select)

    val returnType   = method.termRef.widenTermRefByName.returnType.asType
    val functionType = Ident(method.termRef).etaExpand(method.owner).tpe.asType

    (returnType, functionType) match
      case ('[t], '[ft]) =>
        if result.isExprOf[t] then
          '{
            Stubbed.insertValue[Service, t](MethodId(${ Expr(method.name) }), ${ result.asExprOf[t] })
          }
        else if result.isExprOf[ft] then
          val stubbedFunction = Type.of[ft] match
            case '[a => b]                  => '{ StubResult.makeF(${ result.asExprOf[a => b] }) }
            case '[(a, b) => c]             => '{ StubResult.makeF(${ result.asExprOf[(a, b) => c] }) }
            case '[(a, b, c) => d]          => '{ StubResult.makeF(${ result.asExprOf[(a, b, c) => d] }) }
            case '[(a, b, c, d) => e]       => '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d) => e] }) }
            case '[(a, b, c, d, e) => f]    => '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d, e) => f] }) }
            case '[(a, b, c, d, e, f) => g] => '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d, e, f) => g] }) }
            case '[(a, b, c, d, e, f, g) => h] =>
              '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d, e, f, g) => h] }) }
            case '[(a, b, c, d, e, f, g, h) => i] =>
              '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d, e, f, g, h) => i] }) }
            case '[(a, b, c, d, e, f, g, h, i) => j] =>
              '{ StubResult.makeF(${ result.asExprOf[(a, b, c, d, e, f, g, h, i) => j] }) }

          '{
            Stubbed.insertFunction[Service](MethodId(${ Expr(method.name) }), ${ stubbedFunction })
          }
        else
          // TODO: better error message
          report.errorAndAbort(
            s"Expected ${Type.show[t]} but got ${result.asTerm.tpe.widen.show}"
          )

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
            val argsList = '{ Array(${ Varargs(argss.flatten.collect { case term: Term => term.asExpr }) }*) }
            '{
              $thisExpr.callStubbed(MethodId(${ Expr(methodName) }), ${ argsList })
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
