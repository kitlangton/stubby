package stubby.experimental

import scala.quoted.*

def newClassSym(using Quotes)(
    parent: quotes.reflect.Symbol,
    name: String,
    parents: List[quotes.reflect.TypeRepr],
    decls: quotes.reflect.Symbol => List[quotes.reflect.Symbol],
    selfType: Option[quotes.reflect.TypeRepr]
): quotes.reflect.Symbol =
  val iq = quotes.asInstanceOf[scala.quoted.runtime.impl.QuotesImpl]
  val ir = iq.reflect

  val iparent   = parent.asInstanceOf[ir.Symbol]
  val iparents  = parents.asInstanceOf[List[ir.TypeRepr]]
  val idecls    = decls.asInstanceOf[ir.Symbol => List[ir.Symbol]]
  val iselfType = selfType.asInstanceOf[Option[ir.TypeRepr]]

  val isym = ir.Symbol.newClass(iparent, name, iparents, idecls, iselfType)

  isym.asInstanceOf[quotes.reflect.Symbol]

def ClassDef(using Quotes)(
    cls: quotes.reflect.Symbol,
    parents: List[quotes.reflect.Tree],
    body: List[quotes.reflect.Statement]
): quotes.reflect.ClassDef =
  val iq = quotes.asInstanceOf[scala.quoted.runtime.impl.QuotesImpl]
  val ir = iq.reflect

  val icls     = cls.asInstanceOf[ir.Symbol]
  val iparents = parents.asInstanceOf[List[ir.Tree]]
  val ibody    = body.asInstanceOf[List[ir.Statement]]

  val idef = ir.ClassDef(icls, iparents, ibody)

  idef.asInstanceOf[quotes.reflect.ClassDef]
