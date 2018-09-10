package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.regex.Pattern


object TestsOutputParser {
  const val CONGRATULATIONS = "Congratulations!"
  private const val TEST_OK = "test OK"
  private const val TEST_FAILED = "FAILED + "
  private const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  private val TEST_FAILED_PATTERN: Pattern = Pattern.compile("((.+) )?expected: ?(.*) but was: ?(.*)",
                                                             Pattern.MULTILINE or Pattern.DOTALL)

  @JvmStatic
  fun getCheckResult(messages: List<String>): CheckResult {
    var congratulations = TestsOutputParser.CONGRATULATIONS
    loop@for (message in messages) {
      when {
        TestsOutputParser.TEST_OK in message -> continue@loop
        TestsOutputParser.CONGRATS_MESSAGE in message -> {
          congratulations = message.substringAfter(TestsOutputParser.CONGRATS_MESSAGE)
        }
        TestsOutputParser.TEST_FAILED in message -> {
          return CheckResult(CheckStatus.Failed, message.substringAfter(TestsOutputParser.TEST_FAILED).prettify())
        }
      }
    }

    return CheckResult(CheckStatus.Solved, congratulations)
  }

  private fun String.prettify(): String {
    val matcher = TEST_FAILED_PATTERN.matcher(this)
    return if (matcher.find()) {
      val errorMessage = matcher.group(2)
      val expectedText = matcher.group(3)
      val actualText = matcher.group(4)
      if (errorMessage != null) {
        "$errorMessage\nExpected:\n$expectedText\nActual:\n$actualText"
      } else {
        "Expected:\n$expectedText\nActual:\n$actualText"
      }
    } else {
      this
    }
  }

}
