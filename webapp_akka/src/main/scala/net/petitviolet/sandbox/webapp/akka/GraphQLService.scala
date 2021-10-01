package net.petitviolet.sandbox.webapp.akka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.javadsl.server.RequestEntityExpectedRejection
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.*
import akka.http.scaladsl.unmarshalling.*
import akka.stream.Materializer
import akka.util.ByteString
import io.circe.{Decoder, Json, JsonObject}
import net.petitviolet.sandbox.webapp.akka.common.LoggerProvider
import net.petitviolet.sandbox.webapp.akka.model.*
import net.petitviolet.sandbox.webapp.akka.schema.*
import net.petitviolet.sandbox.webapp.akka.schema.Schema.Context
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe.*
import sangria.marshalling.{InputUnmarshaller, ResultMarshaller}
import sangria.parser.QueryParser

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.deriving.Mirror
import scala.util.{Failure, Success}

trait GraphQLService(
  databaseStore: DatabaseStore,
  tableStore: TableStore,
  columnStore: ColumnStore,
) extends CirceSupport {

  private lazy val schema: sangria.schema.Schema[Context, Unit] = Schema.build(
    Schema.Query(databaseStore, tableStore, columnStore),
    Schema.Mutation(),
  )

  private val graphqlExecutionContext =
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()),
    )

  def execute(graphQLRequest: GraphQLRequest): Async[Json] = {
    val context = Context(graphqlExecutionContext)
    Executor
      .execute(
        schema,
        graphQLRequest.document,
        context,
        root =
          (), // without explicit `()`, it throws; java.lang.NoSuchMethodError: 'scala.runtime.BoxedUnit sangria.execution.Executor$.execute$default$4()'
        operationName = graphQLRequest.operationName,
        variables = graphQLRequest.variables,
      )
  }

}

trait GraphQLRouting { self: CirceSupport with LoggerProvider =>
  given ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  def graphqlPost(graphQLService: GraphQLService): Route = {
    (post & entity(as[GraphQLHttpRequest])) { body =>
      logger.info(s"body: ${body.toString}")
      val GraphQLHttpRequest(queryOpt, varOpt, operationNameOpt) = body
      queryOpt.map { q => QueryParser.parse(q) } match {
        case Some(Success(document)) =>
          val req = GraphQLRequest(
            document,
            varOpt.fold(Json.obj()) {
              identity
            },
            operationNameOpt,
          )
          val result: Future[(StatusCode, Json)] = graphQLService
            .execute(req)
            .map { json =>
              logger.info(s"json: $json")
              StatusCodes.OK -> json
            }
            .recover {
              case error: QueryAnalysisError =>
                StatusCodes.BadRequest -> error.resolveError
              case error: ErrorWithResolver =>
                StatusCodes.InternalServerError -> error.resolveError
            }
          logger.info(s"result: $result")
          complete(result)

        case Some(Failure(error)) =>
          complete(
            StatusCodes.BadRequest,
            GraphQLErrorResponse("failed to parse query", Some(error)),
          )
        case None =>
          complete(
            StatusCodes.BadRequest,
            InvalidRequest("query must be given"),
          )
      }
    }
  }

  def graphiQLGet: Route = {
    inline def acceptHtml = headerValuePF {
      case Accept(mediaRanges) if mediaRanges.exists { mediaRange =>
            mediaRange.isWildcard || mediaRange.matches(MediaTypes.`text/html`)
          } =>
        mediaRanges
    }.flatMap { _ => pass }

    (get & acceptHtml) {
      getFromResource("playground.html")
    }
  }

  private lazy val guard = {
    handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)
  }

  private val rejectionHandler: RejectionHandler = {
    def handle(r: Rejection | RejectionWithOptionalCause) = {
      val t = r match {
        case r: RejectionWithOptionalCause => r.cause
        case _: Rejection                  => None
      }
      complete(
        StatusCodes.UnprocessableEntity,
        GraphQLErrorResponse("[Rejection]cannot be processed", t),
      )
    }

    RejectionHandler
      .newBuilder()
      .handle {
        case r: (MalformedQueryParamRejection |
              MalformedRequestContentRejection |
              RequestEntityExpectedRejection) =>
          handle(r)
      }
      .result()
  }

  private val exceptionHandler = {
    ExceptionHandler { case t =>
      complete(
        StatusCodes.InternalServerError,
        GraphQLErrorResponse("[Exception]Internal Server Error", Some(t)),
      )
    }
  }

}

object GraphQLServiceImpl
    extends GraphQLService(
      TestData.DatabaseStoreImpl,
      TestData.TableStoreImpl,
      TestData.ColumnStoreImpl,
    )

case class GraphQLHttpRequest(
  query: Option[String],
  variables: Option[Json],
  operationName: Option[String],
)
case class GraphQLRequest(
  document: Document,
  variables: Json,
  operationName: Option[String],
)
case class InvalidRequest(message: String, causeOpt: Option[Throwable] = None)
    extends Exception(message, causeOpt.orNull)

case class GraphQLErrorResponse(
  message: String,
  causeOpt: Option[Throwable] = None,
) extends Exception(message, causeOpt.orNull)
