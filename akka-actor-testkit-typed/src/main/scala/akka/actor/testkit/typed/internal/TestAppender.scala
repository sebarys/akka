/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.testkit.typed.internal

import akka.actor.testkit.typed.LoggingEvent
import akka.actor.testkit.typed.scaladsl.LoggingEventFilter
import akka.annotation.InternalApi
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * INTERNAL API
 */
@InternalApi private[akka] object TestAppender {
  private val TestAppenderName = "AkkaTestAppender"

  private def loggerNameOrRoot(loggerName: String): String =
    if (loggerName == "") org.slf4j.Logger.ROOT_LOGGER_NAME else loggerName

  def setupTestAppender(loggerName: String): Unit = {
    val logger = LoggerFactory.getLogger(loggerNameOrRoot(loggerName))
    val logbackLogger = logger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.getAppender(TestAppenderName) match {
      case null =>
        logbackLogger.getLoggerContext
        val testAppender = new TestAppender
        testAppender.setName(TestAppenderName)
        testAppender.setContext(logbackLogger.getLoggerContext)
        testAppender.start()
        logbackLogger.addAppender(testAppender)
      case _: TestAppender =>
      // ok, already setup
      case other =>
        throw new IllegalStateException(s"Unexpected $TestAppenderName already added: $other")
    }
  }

  def addFilter(loggerName: String, filter: LoggingEventFilter): Unit = {
    // FIXME DRY remove duplication between addFilter and removeFilter, and maybe also setup
    val logger = LoggerFactory.getLogger(loggerNameOrRoot(loggerName))
    val logbackLogger = logger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.getAppender(TestAppenderName) match {
      case null =>
        throw new IllegalStateException(s"No $TestAppenderName was setup for logger [${logger.getName}]")
      case testAppender: TestAppender =>
        testAppender.addTestFilter(filter)
      case other =>
        throw new IllegalStateException(
          s"Unexpected $TestAppenderName already added for logger [${logger.getName}]: $other")
    }
  }

  def removeFilter(loggerName: String, filter: LoggingEventFilter): Unit = {
    val logger = LoggerFactory.getLogger(loggerNameOrRoot(loggerName))
    val logbackLogger = logger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.getAppender(TestAppenderName) match {
      case null =>
        throw new IllegalStateException(s"No $TestAppenderName was setup for logger [${logger.getName}]")
      case testAppender: TestAppender =>
        testAppender.removeTestFilter(filter)
      case other =>
        throw new IllegalStateException(
          s"Unexpected $TestAppenderName already added for logger [${logger.getName}]: $other")
    }
  }
}

/**
 * INTERNAL API
 */
@InternalApi private[akka] class TestAppender extends AppenderBase[ILoggingEvent] {

  private var filters: List[LoggingEventFilter] = Nil

  // invocations are synchronized via doAppend in AppenderBase
  override def append(event: ILoggingEvent): Unit = {
    import scala.collection.JavaConverters._

    val throwable = event.getThrowableProxy match {
      case p: ThrowableProxy =>
        Option(p.getThrowable)
      case _ => None
    }

    val loggingEvent = LoggingEvent(
      level = convertLevel(event.getLevel),
      message = event.getFormattedMessage,
      loggerName = event.getLoggerName,
      threadName = event.getThreadName,
      timeStamp = event.getTimeStamp,
      marker = Option(event.getMarker),
      throwable = throwable,
      mdc = event.getMDCPropertyMap.asScala.toMap)

    filter(loggingEvent)
  }

  private def convertLevel(level: ch.qos.logback.classic.Level): Level = {
    level.levelInt match {
      case ch.qos.logback.classic.Level.TRACE_INT => Level.TRACE
      case ch.qos.logback.classic.Level.DEBUG_INT => Level.DEBUG
      case ch.qos.logback.classic.Level.INFO_INT  => Level.INFO
      case ch.qos.logback.classic.Level.WARN_INT  => Level.WARN
      case ch.qos.logback.classic.Level.ERROR_INT => Level.ERROR
      case _ =>
        throw new IllegalArgumentException("Level " + level.levelStr + ", " + level.levelInt + " is unknown.")
    }
  }

  private def filter(event: LoggingEvent): Boolean = {
    filters.exists(f =>
      try {
        f.apply(event)
      } catch {
        case _: Exception => false
      })
  }

  def addTestFilter(filter: LoggingEventFilter): Unit = synchronized {
    filters ::= filter
  }

  def removeTestFilter(filter: LoggingEventFilter): Unit = synchronized {
    @scala.annotation.tailrec
    def removeFirst(list: List[LoggingEventFilter], zipped: List[LoggingEventFilter] = Nil): List[LoggingEventFilter] =
      list match {
        case head :: tail if head == filter => tail.reverse_:::(zipped)
        case head :: tail                   => removeFirst(tail, head :: zipped)
        case Nil                            => filters // filter not found, just return original list
      }
    filters = removeFirst(filters)
  }

}
