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
import scala.concurrent.{ExecutionContext, Future}
import scala.deriving.Mirror

object AkkaHttpWebApp extends App with Service(GraphQLServiceImpl):
  given system: ActorSystem = ActorSystem()
  given executionContext: ExecutionContext = system.dispatcher

  override val config = ConfigFactory.load()
  override val logger = Logging(system, "AkkaHttpWebApp")

  Http()
    .newServerAt(config.getString("http.interface"), config.getInt("http.port"))
    .bindFlow(routes)
end AkkaHttpWebApp

case class Message(text: String)

trait Service(graphQLService: GraphQLService) extends CirceSupport:
  given system: ActorSystem
  given executionContext: ExecutionContext

  def config: Config
  val logger: LoggingAdapter

  val routes: Route = {
    logRequestResult("webapp-akka") {
      pathPrefix("graphql") {
        ???
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
