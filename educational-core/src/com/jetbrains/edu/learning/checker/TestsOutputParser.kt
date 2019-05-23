package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.checker.CheckUtils.STUDY_PREFIX
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.regex.Pattern


object TestsOutputParser {
  const val CONGRATULATIONS = "Congratulations!"
  const val TEST_OK = "test OK"
  const val TEST_FAILED = "FAILED + "
  const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  private val TEST_FAILED_PATTERN: Pattern = Pattern.compile("((.+) )?expected: ?(.*) but was: ?(.*)",
                                                             Pattern.MULTILINE or Pattern.DOTALL)

  @JvmOverloads
  @JvmStatic
  fun getCheckResult(messages: List<String>, needEscapeResult: Boolean = true): CheckResult {
    var congratulations = CONGRATULATIONS
    loop@ for ((index, message) in messages.withIndex()) {
      when {
        TEST_OK in message -> continue@loop
        CONGRATS_MESSAGE in message -> {
          congratulations = message.substringAfter(CONGRATS_MESSAGE)
        }
        TEST_FAILED in message -> {
          val builder = StringBuilder(message.substringAfter(TEST_FAILED))
          for (j in index + 1 until messages.size) {
            val failedTextLine = messages[j]
            if (failedTextLine.contains(STUDY_PREFIX) &&
                (failedTextLine.contains(CONGRATS_MESSAGE) || failedTextLine.contains(TEST_OK))) {
              break
            }
            builder.append("\n")
            builder.append(failedTextLine.substringAfter(STUDY_PREFIX))
          }
          return CheckResult(CheckStatus.Failed, builder.toString().prettify(), needEscape = needEscapeResult)
        }
      }
    }

    return CheckResult(CheckStatus.Solved, congratulations, needEscape = needEscapeResult)
  }

  private fun String.prettify(): String {
    val matcher = TEST_FAILED_PATTERN.matcher(this)
    return if (matcher.find()) {
      val errorMessage = matcher.group(2)
      val expectedText = matcher.group(3)
      val actualText = matcher.group(4)
      if (errorMessage != null) {
        "$errorMessage\nExpected:\n$expectedText\nActual:\n$actualText"
      }
      else {
        "Expected:\n$expectedText\nActual:\n$actualText"
      }
    }
    else {
      this
    }
  }

}
