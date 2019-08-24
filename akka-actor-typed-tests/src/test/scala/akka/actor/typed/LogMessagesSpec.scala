/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.typed

import akka.actor
import akka.actor.testkit.typed.scaladsl.LoggingEventFilter
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import org.scalatest.WordSpecLike
import org.slf4j.event.Level

class LogMessagesSpec extends ScalaTestWithActorTestKit("""
    akka.loglevel = DEBUG # test verifies debug
    """) with WordSpecLike {

  implicit val untyped: actor.ActorSystem = system.toUntyped

  "The log messages behavior" should {

    "log messages and signals" in {
      val behavior: Behavior[String] = Behaviors.logMessages(Behaviors.ignore)

      val ref: ActorRef[String] = spawn(behavior)

      // FIXME not testing `source = ref.path.toString`

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received message Hello", occurrences = 1).intercept {
        ref ! "Hello"
      }

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received signal PostStop", occurrences = 1).intercept {
        testKit.stop(ref)
      }
    }

    "log messages with provided log level" in {
      val opts = LogOptions().withLevel(Level.INFO)
      val behavior: Behavior[String] = Behaviors.logMessages(opts, Behaviors.ignore)

      val ref: ActorRef[String] = spawn(behavior)

      LoggingEventFilter.info(s"actor ${ref.path.toString} received message Hello", occurrences = 1).intercept {
        ref ! "Hello"
      }

      LoggingEventFilter.info(s"actor ${ref.path.toString} received signal PostStop", occurrences = 1).intercept {
        testKit.stop(ref)
      }
    }

    "log messages with provided logger" in {
      val logger = system.log
      val opts = LogOptions().withLogger(logger)
      val behavior: Behavior[String] = Behaviors.logMessages(opts, Behaviors.ignore)

      val ref: ActorRef[String] = spawn(behavior)

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received message Hello", occurrences = 1).intercept {
        ref ! "Hello"
      }

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received signal PostStop", occurrences = 1).intercept {
        testKit.stop(ref)
      }
    }

    "not log messages when not enabled" in {
      val opts = LogOptions().withEnabled(false)
      val behavior: Behavior[String] = Behaviors.logMessages(opts, Behaviors.ignore)

      val ref: ActorRef[String] = spawn(behavior)

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received message Hello", occurrences = 0).intercept {
        ref ! "Hello"
      }

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received signal PostStop", occurrences = 0).intercept {
        testKit.stop(ref)
      }
    }

    "log messages with decorated MDC values" in {
      val opts = LogOptions().withLevel(Level.DEBUG)
      val mdc = Map("mdc" -> "true")
      val behavior = Behaviors.withMdc[String](mdc)(Behaviors.logMessages(opts, Behaviors.ignore))

      val ref = spawn(behavior)
      LoggingEventFilter
        .debug(s"actor ${ref.path.toString} received message Hello MDC is $mdc", occurrences = 1)
        .intercept(ref ! "Hello")

      LoggingEventFilter
        .debug(s"actor ${ref.path.toString} received signal PostStop MDC is $mdc", occurrences = 1)
        .intercept(testKit.stop(ref))
    }

    "log messages with different decorated MDC values in different actors" in {
      val opts = LogOptions().withLevel(Level.DEBUG)
      val mdc1 = Map("mdc" -> "true")
      val behavior1 = Behaviors.withMdc[String](mdc1)(Behaviors.logMessages(opts, Behaviors.ignore))
      val mdc2 = Map("mdc" -> "false")
      val behavior2 = Behaviors.withMdc[String](mdc2)(Behaviors.logMessages(opts, Behaviors.ignore))

      val ref2 = spawn(behavior2)
      LoggingEventFilter
        .debug(s"actor ${ref2.path.toString} received message Hello MDC is $mdc2", occurrences = 1)
        .intercept(ref2 ! "Hello")

      val ref1 = spawn(behavior1)
      LoggingEventFilter
        .debug(s"actor ${ref1.path.toString} received message Hello MDC is $mdc1", occurrences = 1)
        .intercept(ref1 ! "Hello")

      LoggingEventFilter
        .debug(s"actor ${ref2.path.toString} received signal PostStop MDC is $mdc2", occurrences = 1)
        .intercept(testKit.stop(ref2))

      LoggingEventFilter
        .debug(s"actor ${ref1.path.toString} received signal PostStop MDC is $mdc1", occurrences = 1)
        .intercept(testKit.stop(ref1))

    }

    "log messages of different type" in {
      val behavior: Behavior[String] = Behaviors.logMessages(Behaviors.ignore[String])

      val ref = spawn(behavior)

      LoggingEventFilter.debug(s"actor ${ref.path.toString} received message 13", occurrences = 1).intercept {
        ref.unsafeUpcast[Any] ! 13
      }
    }

  }
}
