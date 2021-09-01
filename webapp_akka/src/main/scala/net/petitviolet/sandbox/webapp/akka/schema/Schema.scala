package net.petitviolet.sandbox.webapp.akka.schema
import net.petitviolet.sandbox.webapp.akka.model
import net.petitviolet.sandbox.webapp.akka.model.*
import sangria.schema.*

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.util.Try

object Schema {
  case class Context(executionContext: ExecutionContext)

  given DateTimeType: ScalarType[ZonedDateTime] = {
    import sangria.validation._
    case object ZonedDateTimeCoercionViolation
        extends ValueCoercionViolation("DateTime expected")

    val formatStr = "yyyy-MM-dd HH:mm:ss"
    val format = DateTimeFormatter.ofPattern(formatStr)

    val convertString: String => Either[Violation, ZonedDateTime] = str =>
      Try { ZonedDateTime.parse(str, format) }.toEither.left.flatMap { _ =>
        Left(ZonedDateTimeCoercionViolation)
      }

    ScalarType[ZonedDateTime](
      "DateTime",
      description = Some(s"DateTime scalar type. format = $formatStr"),
      coerceOutput = { (date, _) =>
        date.format(format)
      },
      coerceUserInput = { input =>
        StringType.coerceUserInput(input).flatMap(convertString)
      },
      coerceInput = { input =>
        StringType.coerceInput(input).flatMap(convertString)
      },
    )
  }

  class Query(
    databaseStore: DatabaseStore,
    tableStore: TableStore,
    columnStore: ColumnStore,
  )(using ec: ExecutionContext) {
    // cannot use macro in Scala3
    // val DatabaseType = deriveObjectType[Context, Database]()
    // given seems to be equal to `implicit lazy val`
    given DatabaseType: ObjectType[Unit, Database] = ObjectType(
      "Database",
      "database",
      fields[Unit, Database](
        Field("id", StringType, resolve = _.value.id.value),
        Field("name", StringType, resolve = _.value.name.value),
        Field("updatedAt", DateTimeType, resolve = _.value.updatedAt),
        Field(
          "tables",
          ListType(TableType),
          resolve = { ctx =>
            tableStore.findAllByDatabaseId(ctx.value.id)
          },
        ),
      ),
    )

    given TableType: ObjectType[Unit, Table] = ObjectType(
      "Table",
      "table",
      fields[Unit, Table](
        Field("id", StringType, resolve = _.value.id.value),
        Field("name", StringType, resolve = _.value.name.value),
        Field("updatedAt", DateTimeType, resolve = _.value.updatedAt),
        Field(
          "database",
          OptionType(DatabaseType),
          resolve = { ctx =>
            databaseStore.findById(ctx.value.databaseId)
          },
        ),
        Field(
          "columns",
          ListType(ColumnType),
          resolve = { ctx =>
            columnStore.findAllByTableId(ctx.value.id)
          },
        ),
      ),
    )

    given ColumnType: ObjectType[Unit, Column] = {
      val ColumnTypeType = EnumType[model.ColumnType](
        "ColumnType",
        Some("column type"),
        List(
          EnumValue("Int", value = model.ColumnType.Int),
          EnumValue("Long", value = model.ColumnType.Long),
          EnumValue("Varchar", value = model.ColumnType.Varchar),
        ),
      )
      ObjectType(
        "Column",
        "column",
        fields[Unit, Column](
          Field("id", StringType, resolve = _.value.id.value),
          Field("name", StringType, resolve = _.value.name.value),
          Field("type", ColumnTypeType, resolve = _.value.columnType),
          Field(
            "table",
            OptionType(TableType),
            resolve = { ctx =>
              tableStore.findById(ctx.value.tableId)
            },
          ),
        ),
      )
    }
  }

}
