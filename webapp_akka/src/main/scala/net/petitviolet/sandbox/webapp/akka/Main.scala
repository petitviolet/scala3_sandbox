package net.petitviolet.sandbox.webapp.akka

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{
  FromRequestUnmarshaller,
  Unmarshal,
  Unmarshaller,
}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.{jawn, Decoder, Json}
import net.petitviolet.sandbox.webapp.akka.AkkaHttpWebApp.{logger, system}

import java.io.IOException
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.deriving.Mirror
import scala.util.chaining.scalaUtilChainingOps
import scala.util.{Failure, Success}

object AkkaHttpWebApp extends App with Service(GraphQLServiceImpl):
  given system: ActorSystem = ActorSystem("AkkaHttpWebApp")
  given executionContext: ExecutionContext = system.dispatcher
  override val graphqlExecutionContext =
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()),
    )

  val config = ConfigFactory.load()
  override val logger = Logging(system, "AkkaHttpWebApp")

  run(config)

end AkkaHttpWebApp

case class Message(text: String)

trait Service(graphQLService: GraphQLService)
    extends CirceSupport
    with GraphQLRouting:
  def graphqlExecutionContext: ExecutionContext

  def logger: LoggingAdapter

  def run(config: Config): ActorSystem ?=> ExecutionContext ?=> Unit = {
    val interface = config.getString("http.interface")
    val host = config.getInt("http.port")
    val binding = Http()
      .newServerAt(interface, host)
      .bindFlow(routes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))(
        ExecutionContext.global,
      )

    binding.onComplete {
      case Success(b) =>
        logger.info(s"Server started at ${interface}:${host}!")
      case Failure(e) =>
        logger.error(s"failed to bind", e)
        system.terminate()
    }

//    scala.io.StdIn.readLine(
//      s"Server started at ${interface}:${host}\nPress enter key to stop...\n",
//    )
//
//    logger.info(s"Started termination process...")
//
//    binding
//      .flatMap(_.unbind())(ExecutionContext.global)
//      .flatMap { _ =>
//        logger.info(s"Binding has been terminated")
//        system.terminate()
//      }(ExecutionContext.global)
//      .map { _ => akka.Done }(ExecutionContext.global)
//      .tap {
//        _.onComplete {
//          case Success(binding) =>
//            logger.info(s"system has been terminated")
//          case Failure(ex) =>
//            logger.error(s"Error has happened: $ex", ex)
//        }(ExecutionContext.global)
//      }
//      .pipe { Await.ready(_, 11.seconds) }
  }

  lazy val routes: Route = {
    logRequestResult("webapp-akka", Logging.WarningLevel) {
      path("graphql") {
        graphiQLGet
          ~ graphqlPost(graphQLService)(using graphqlExecutionContext)
      } ~
        pathPrefix("echo") {
          (get & path(Segment)) { path =>
            complete { path }
          }
        } ~
        pathPrefix("ping") {
          (post & entity(as[Message])) { message =>
            complete { message.text }
          }
        }
    }
  }
end Service
