package net.petitviolet.sandbox.webapp.akka.model

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.annotation.targetName

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
  def findById(databaseId: DatabaseId): Async[Option[Database]]

trait TableStore:
  def findById(tableId: TableId): Async[Option[Table]]
  def findAllByDatabaseId(databaseId: DatabaseId): Async[Seq[Table]]

trait ColumnStore:
  def findAllByTableId(tableId: TableId): Async[Seq[Column]]
