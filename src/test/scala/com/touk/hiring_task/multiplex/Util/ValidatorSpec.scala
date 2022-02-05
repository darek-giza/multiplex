package com.touk.hiring_task.multiplex.Util

import com.touk.hiring_task.multiplex.CinemaConstants
import com.touk.hiring_task.multiplex.commons._
import com.touk.hiring_task.multiplex.utils.{SystemClock, ValidatorImpl}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ValidatorSpec extends AnyFreeSpec with Matchers with SystemClock {

  sealed trait ValidatorConstants extends CinemaConstants {
    val now = currentMillis()
    val tenMinutesAgo = nowMillis().minusMinutes(10).toInstant.toEpochMilli
    val plusTenMinutesAgo = nowMillis().plusMinutes(10).toInstant.toEpochMilli
    val plusTwentyMinutesAgo = nowMillis().plusMinutes(20).toInstant.toEpochMilli

    def tooDistant(days: Int) = nowMillis().plusDays(days).toInstant.toEpochMilli
  }

  sealed trait TestContext extends ValidatorConstants {
    val validator = new ValidatorImpl
  }

  "validator should check" - {
    "isNotPastDate" - {
      "return fals when start date is past" in new TestContext {
        validator.isNotPastDate(tenMinutesAgo) shouldBe false
      }

      "return true when start date is past" in new TestContext {
        validator.isNotPastDate(plusTenMinutesAgo) shouldBe true
      }
    }

    "isNotTooDistantDate" - {
      "return false when date is greater then 7 days" in new TestContext {
        validator.isNotTooDistantDate(tooDistant(8)) shouldBe false
      }

      "return true when date is less then 7 days" in new TestContext {
        validator.isNotTooDistantDate(tooDistant(6)) shouldBe true
      }
    }

    "isStartAtLessFinishAt" - {
      "return false when start date is greater then finish date" in new TestContext {
        validator.isStartAtLessFinishAt(plusTenMinutesAgo, tenMinutesAgo) shouldBe false
      }

      "return true when start date is greater then finish date" in new TestContext {
        validator.isStartAtLessFinishAt(tenMinutesAgo, plusTenMinutesAgo) shouldBe true
      }
    }

    "isNotExpired" - {
      "return false when projection start for less then 15 minutes" in new TestContext {
        validator.isNotExpired(plusTenMinutesAgo) shouldBe false
      }

      "return true when projection start for more then 15 minutes" in new TestContext {
        validator.isNotExpired(plusTwentyMinutesAgo) shouldBe true
      }
    }

    "isListPlaceNonEmpty" - {
      "return false when list ordered place is empty" in new TestContext {
        validator.isListPlaceNonEmpty(Nil) shouldBe false
      }

      "return true when list ordered place not empty" in new TestContext {
        validator.isListPlaceNonEmpty(listPlaceDto) shouldBe true
      }
    }

    "isUserNameValid" - {
      "return ex when first name to short" in new TestContext {
        validator.isUserNameValid("Joh Black-White") shouldBe Left(InvalidFirstNameLengthException)
      }

      "return ex when firs family name to short" in new TestContext {
        validator.isUserNameValid("John Bla") shouldBe Left(InvalidFamilyNameLengthException)
      }

      "return ex when family name to short" in new TestContext {
        validator.isUserNameValid("John Bla-White") shouldBe Left(InvalidFamilyNameLengthException)
      }

      "return ex when second family name to short" in new TestContext {
        validator.isUserNameValid("John Black-Whi") shouldBe Left(InvalidSecondFamilyNameLengthException)
      }

      "return ex when first name has invalid format" in new TestContext {
        validator.isUserNameValid("john Black-White") shouldBe Left(InvalidFirstNameFormatException)
      }

      "return ex when first family name has invalid format" in new TestContext {
        validator.isUserNameValid("John black") shouldBe Left(InvalidFamilyNameFormatException)
      }

      "return ex when family name has invalid format" in new TestContext {
        validator.isUserNameValid("John black-White") shouldBe Left(InvalidFamilyNameFormatException)
      }

      "return ex when second family name has invalid format" in new TestContext {
        validator.isUserNameValid("John Black-white") shouldBe Left(InvalidSecondFamilyNameFormatException)
      }

      "return name when name has correct length and format" in new TestContext {
        validator.isUserNameValid("John Black") shouldBe Right("John Black")
      }

      "return userName when it has correct length and format" in new TestContext {
        validator.isUserNameValid("John Black-White") shouldBe Right("John Black-White")
      }
    }
  }
}
