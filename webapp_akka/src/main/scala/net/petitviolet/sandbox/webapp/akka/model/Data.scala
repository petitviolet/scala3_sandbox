package net.petitviolet.sandbox.webapp.akka.model

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.annotation.targetName

object TestData:
  val databases: Seq[Database] = Database.create(DatabaseName("db-1")) ::
    Database.create(DatabaseName("db-2")) ::
    Database.create(DatabaseName("db-3")) :: Nil

  val tables: Seq[Table] = databases.zipWithIndex.flatMap { (database, i) =>
    (0.to(scala.util.Random.nextInt(5))).map { j =>
      Table.create(database.id, TableName(s"table-${i}-${j}"))
    }
  }

  val columns: Seq[Column] = tables.zipWithIndex.flatMap { (table, i) =>
    (0.to(scala.util.Random.nextInt(10))).map { j =>
      Column.create(
        table.id,
        ColumnName(s"column-${i}-${j}"),
        ColumnType.values((i + j) % ColumnType.values.length),
      )
    }
  }

  object DatabaseStoreImpl extends DatabaseStore {
    override def findById(databaseId: DatabaseId): Async[Option[Database]] =
      Future { databases.find { _.id == databaseId } }

    override def findAll(): Async[Seq[Database]] = Future.successful {
      databases
    }
  }

  object TableStoreImpl extends TableStore {
    override def findById(tableId: TableId): Async[Option[Table]] = Future {
      tables.find { _.id == tableId }
    }

    override def findAllByDatabaseId(
      databaseId: DatabaseId,
    ): Async[Seq[Table]] = Future {
      tables.filter { _.databaseId == databaseId }
    }
  }

  object ColumnStoreImpl extends ColumnStore {
    override def findAllByTableId(tableId: TableId): Async[Seq[Column]] =
      Future { columns.filter { _.tableId == tableId } }
  }

end TestData

case class Database(
  id: DatabaseId,
  name: DatabaseName,
  updatedAt: ZonedDateTime,
)
opaque type DatabaseId = String
object DatabaseId:
  def apply(id: String): DatabaseId = id
  def generate = apply(UUID.randomUUID().toString)

extension (a: DatabaseId) @targetName("a_DatabaseId") def value: String = a

opaque type DatabaseName = String
object DatabaseName:
  def apply(name: String): DatabaseName = name

extension (a: DatabaseName) @targetName("a_DatabaseName") def value: String = a

object Database:
  def create(name: DatabaseName): Database =
    apply(DatabaseId.generate, name, ZonedDateTime.now())

case class Table(
  id: TableId,
  databaseId: DatabaseId,
  name: TableName,
  updatedAt: ZonedDateTime,
)
object Table:
  def create(databaseId: DatabaseId, name: TableName): Table =
    apply(TableId.generate, databaseId, name, ZonedDateTime.now())

opaque type TableId = String
object TableId:
  def apply(id: String): TableId = id
  def generate = apply(UUID.randomUUID().toString)

extension (a: TableId) @targetName("a_TableId") def value: String = a

opaque type TableName = String
object TableName:
  def apply(name: String): TableName = name

extension (a: TableName) @targetName("a_TableName") def value: String = a

case class Column(
  id: ColumnId,
  tableId: TableId,
  name: ColumnName,
  columnType: ColumnType,
)
object Column:
  def create(
    tableId: TableId,
    name: ColumnName,
    columnType: ColumnType,
  ): Column =
    apply(ColumnId.generate, tableId, name, columnType)

opaque type ColumnId = String
object ColumnId:
  def apply(id: String): ColumnId = id
  def generate = apply(UUID.randomUUID().toString)
extension (a: ColumnId) @targetName("a_ColumnId") def value: String = a

opaque type ColumnName = String
object ColumnName:
  def apply(name: String): ColumnName = name
extension (a: ColumnName) @targetName("a_ColumnName") def value: String = a

enum ColumnType {
  case Int, Long, Varchar
}

type Async = [T] =>> (ExecutionContext ?=> Future[T])

trait DatabaseStore:
  def findAll(): Async[Seq[Database]]
  def findById(databaseId: DatabaseId): Async[Option[Database]]

trait TableStore:
  def findById(tableId: TableId): Async[Option[Table]]
  def findAllByDatabaseId(databaseId: DatabaseId): Async[Seq[Table]]

trait ColumnStore:
  def findAllByTableId(tableId: TableId): Async[Seq[Column]]
