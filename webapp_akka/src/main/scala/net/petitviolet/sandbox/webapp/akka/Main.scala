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

trait Service(graphQLService: GraphQLService):
  given system: ActorSystem
  given executionContext: ExecutionContext

  def config: Config
  val logger: LoggingAdapter

  // raise `Compiler bug: `constValue` was not evaluated by the compiler`
  // given decoder[T]: (Mirror.Of[T] ?=> Decoder[T]) = io.circe.generic.semiauto.deriveDecoder
  given Decoder[Message] = io.circe.generic.semiauto.deriveDecoder
  type JsonUnmarshaller = [T] =>> (Decoder[T] ?=> Unmarshaller[HttpEntity, T])
  given [T]: JsonUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller.map { (bs: ByteString) =>
      jawn
        .parseByteBuffer(bs.asByteBuffer)
        .flatMap { summon[Decoder[T]].decodeJson }
        .fold(throw _, identity)
    }

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
