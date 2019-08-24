/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.testkit.typed

import scala.compat.java8.OptionConverters._

import java.util.Optional

import org.slf4j.Marker
import org.slf4j.event.Level

object LoggingEvent {
  def apply(level: Level, loggerName: String, threadName: String, message: String, timeStamp: Long): LoggingEvent =
    new LoggingEvent(level, loggerName, threadName, message, timeStamp, None, None, Map.empty)
}

final case class LoggingEvent(
    level: Level,
    loggerName: String,
    threadName: String,
    message: String,
    timeStamp: Long,
    marker: Option[Marker],
    throwable: Option[Throwable],
    mdc: Map[String, String]) {

  /**
   * Java API
   */
  def getMarker: Optional[Marker] =
    marker.asJava

  /**
   * Java API
   */
  def getThrowable: Optional[Throwable] =
    throwable.asJava

  /**
   * Java API
   */
  def getMdc: java.util.Map[String, String] = {
    import scala.collection.JavaConverters._
    mdc.asJava
  }

}
