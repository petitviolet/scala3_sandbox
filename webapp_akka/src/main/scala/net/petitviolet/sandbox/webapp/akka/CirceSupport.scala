package net.petitviolet.sandbox.webapp.akka

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe.{jawn, Decoder}

import scala.deriving.Mirror

trait CirceSupport {
  // raise `Compiler bug: `constValue` was not evaluated by the compiler`
  // given decoder[T]: (Mirror.Of[T] ?=> Decoder[T]) = io.circe.generic.semiauto.deriveDecoder
  type JsonUnmarshaller = [T] =>> (Decoder[T] ?=> Unmarshaller[HttpEntity, T])

  given [T]: JsonUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller.map { (bs: ByteString) =>
      jawn
        .parseByteBuffer(bs.asByteBuffer)
        .flatMap { summon[Decoder[T]].decodeJson }
        .fold(throw _, identity)
    }

  inline
  given [T](using inline A: Mirror.Of[T]): Decoder[T] =
    io.circe.generic.semiauto.deriveDecoder(using A)

}
