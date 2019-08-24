/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.testkit.typed.scaladsl

import akka.actor.testkit.typed.TestException
import org.scalatest.WordSpecLike
import org.slf4j.LoggerFactory

class TestAppenderSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  private val log = LoggerFactory.getLogger(getClass)

  "TestAppender and LoggingEventFilter" must {
    "filter errors without cause" in {
      LoggingEventFilter.error(message = "an error", occurrences = 2).intercept {
        log.error("an error")
        log.error("an error")
      }
    }

    "filter errors with cause" in {
      LoggingEventFilter[TestException](message = "err", occurrences = 1).intercept {
        log.error("err", TestException("an error"))
      }
    }

    "filter warnings" in {
      LoggingEventFilter.warning(message = "a warning", occurrences = 2).intercept {
        log.error("an error")
        log.warn("a warning")
        log.error("an error")
        log.warn("a warning")
      }
    }

  }

}
