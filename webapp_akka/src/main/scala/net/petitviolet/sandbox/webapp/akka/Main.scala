package net.petitviolet.sandbox.webapp.akka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{
  FromRequestUnmarshaller,
  Unmarshal,
  Unmarshaller,
}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.circe.{jawn, Decoder, Json}

import scala.util.chaining.scalaUtilChainingOps
import java.io.IOException
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.deriving.Mirror
import concurrent.duration.DurationInt

object AkkaHttpWebApp extends App with Service(GraphQLServiceImpl):
  given system: ActorSystem = ActorSystem()
  given executionContext: ExecutionContext = system.dispatcher
  override val graphqlExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  override val config = ConfigFactory.load()
  override val logger = Logging(system, "AkkaHttpWebApp")

  def main = {
    val interface = config.getString("http.interface")
    val host = config.getInt("http.port")
    val binding = Http()
      .newServerAt(interface, host)
      .bindFlow(routes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    logger.info(s"Server started at ${interface}:${host}\n")
    scala.io.StdIn.readLine("Press enter key to stop...\n")

    val x = binding
      .flatMap(_.unbind())(ExecutionContext.global)
      .tap { _.onComplete { _ => system.terminate() }(ExecutionContext.global) }

    Await.ready(x, 5.seconds)
    sys.runtime.gc()
    println(s"shutdown completed!\n")
  }

  main

end AkkaHttpWebApp

case class Message(text: String)

trait Service(graphQLService: GraphQLService)
    extends CirceSupport
    with GraphQLRouting:
  given system: ActorSystem
  given executionContext: ExecutionContext
  def graphqlExecutionContext: ExecutionContext

  def config: Config
  def logger: LoggingAdapter

  lazy val routes: Route = {
    logRequestResult("webapp-akka", Logging.WarningLevel) {
      path("graphql") {
          graphqlPost(graphQLService)(using graphqlExecutionContext)
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
