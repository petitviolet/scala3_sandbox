package net.petitviolet.sandbox.webapp.akka

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes}
import akka.http.scaladsl.unmarshalling.{
  FromByteStringUnmarshaller,
  FromEntityUnmarshaller,
  Unmarshaller,
}
import akka.util.ByteString
import io.circe.*

import scala.concurrent.Future
import scala.deriving.Mirror

// Strongly recommend to use https://github.com/hseeberger/akka-http-json
trait CirceSupport {
  given decodeNullable[A: Decoder]: Decoder[Option[A]] = Decoder.decodeOption[A]
  given decodeJson: Decoder[Json] = Decoder.decodeJson

  // raise `Compiler bug: `constValue` was not evaluated by the compiler`
  // given decoder[T]: (Mirror.Of[T] ?=> Decoder[T]) = io.circe.generic.semiauto.deriveDecoder
  type JsonUnmarshaller = [T] =>> (Decoder[T] ?=> FromEntityUnmarshaller[T])

  given jsonUnmarshaller[T]: JsonUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller.map { (bs: ByteString) =>
      jawn
        .parseByteBuffer(bs.asByteBuffer)
        .flatMap { summon[Decoder[T]].decodeJson }
        .fold(throw _, identity)
    }

  inline
  given [T](using inline A: Mirror.Of[T]): Decoder[T] =
    io.circe.generic.semiauto.deriveDecoder(using A)

  type JsonMarshaller = [T] =>> (Encoder[T] ?=> ToEntityMarshaller[T])

  given [T]: JsonMarshaller[T] = {
    val contentType = ContentTypes.`application/json`
    Marshaller.withFixedContentType(contentType) { t =>
      HttpEntity(
        contentType,
        ByteString(
          Printer.noSpaces.printToByteBuffer(
            summon[Encoder[T]].apply(t),
            contentType.charset.nioCharset(),
          ),
        ),
      )
    }
  }

  inline
  given [T](using inline A: Mirror.Of[T]): Encoder[T] =
    io.circe.generic.semiauto.deriveEncoder(using A)
}
