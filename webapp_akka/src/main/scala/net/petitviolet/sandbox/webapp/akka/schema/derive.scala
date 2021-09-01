package net.petitviolet.sandbox.webapp.akka.schema

import sangria.schema.{Field, ObjectType}

object derive {
  import scala.quoted.*
  inline def deriveObjectType[Ctx, Val](
    inline additionalFields: Seq[Field[Ctx, Val]],
  ): ObjectType[Ctx, Val] = {
    ${ deriveObjectTypeImpl[Ctx, Val]('additionalFields) }
  }

  private def deriveObjectTypeImpl[Ctx: Type, Val: Type](
    additionalFields: Expr[Seq[Field[Ctx, Val]]],
  )(using Quotes): Expr[ObjectType[Ctx, Val]] = '{ ??? }

}
