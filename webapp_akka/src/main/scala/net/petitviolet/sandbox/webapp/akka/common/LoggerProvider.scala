package net.petitviolet.sandbox.webapp.akka.common

import org.slf4j.LoggerFactory

trait LoggerProvider {
  final protected lazy val logger = LoggerFactory.getLogger(this.getClass)
}
