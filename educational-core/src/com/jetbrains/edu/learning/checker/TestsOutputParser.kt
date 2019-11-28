package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATS_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.checker.CheckUtils.STUDY_PREFIX
import com.jetbrains.edu.learning.checker.CheckUtils.TEST_FAILED
import com.jetbrains.edu.learning.checker.CheckUtils.TEST_OK
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.regex.Pattern

class TestsOutputParser {

  private var pendingFailedTestMessage: StringBuilder = StringBuilder()
  private var lastFailedMessage: TestMessage.Failed? = null
  private var congratulations: String = CONGRATULATIONS

  @JvmOverloads
  fun getCheckResult(messages: List<String>, needEscapeResult: Boolean = true): CheckResult {
    val processor: (TestMessage) -> Unit = { message ->
      when (message) {
        is TestMessage.Congrats -> {
            congratulations = message.congratulations
        }
        is TestMessage.Failed -> {
          lastFailedMessage = message
        }
      }
    }

    for (message in messages) {
      processMessage(message, processor)
      if (lastFailedMessage != null) break
    }
    processPendingFailedMessage(processor)

    val finalFailedMessage = lastFailedMessage
    return if (finalFailedMessage != null) {
      val diff = finalFailedMessage.diff
      CheckResult(CheckStatus.Failed, finalFailedMessage.message, needEscape = needEscapeResult, diff = diff)
    } else {
      CheckResult(CheckStatus.Solved, congratulations, needEscape = needEscapeResult)
    }
  }

  fun processMessage(message: String, processor: (TestMessage) -> Unit) {
    // Pass each line of output to processor as is to show them in console, for example
    processor(TestMessage.TextLine(message))
    if (!message.startsWith(STUDY_PREFIX)) {
      // If line doesn't started with STUDY_PREFIX then previous failed message is fully read
      // and can be processed
      processPendingFailedMessage(processor)
    } else {
      when {
        TEST_OK in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is a new ok message
          processPendingFailedMessage(processor)
          val name = message.substringAfter("$STUDY_PREFIX ").substringBefore(" $TEST_OK")
          processor(TestMessage.Ok(name))
        }
        CONGRATS_MESSAGE in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is a new congrats message
          processPendingFailedMessage(processor)
          processor(TestMessage.Congrats(message.substringAfter(CONGRATS_MESSAGE)))
        }
        TEST_FAILED in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is the first line of new failed message
          processPendingFailedMessage(processor)
          pendingFailedTestMessage.append(message.substringAfter("$STUDY_PREFIX ").removeSuffix("\n"))
        }
        else -> {
          // Append secondary lines of multiline failed message
          pendingFailedTestMessage.append("\n")
          pendingFailedTestMessage.append(message.substringAfter("$STUDY_PREFIX ").removeSuffix("\n"))
        }
      }
    }
  }

  private fun processPendingFailedMessage(processor: (TestMessage) -> Unit) {
    if (pendingFailedTestMessage.isEmpty()) return
    val fullMessage = pendingFailedTestMessage.toString()
    // Our custom python test framework produces test name before `TEST_FAILED`
    val rawTestName = fullMessage.substringBefore(TEST_FAILED, "").trim()
    val testName = if (rawTestName.isEmpty()) "test" else rawTestName
    val message = fullMessage.substringAfter(TEST_FAILED)
    val matcher = TEST_FAILED_PATTERN.matcher(message)
    val testMessage = if (matcher.find()) {
      val errorMessage = matcher.group(2) ?: ""
      val expectedText = matcher.group(3)
      val actual = matcher.group(4)
      TestMessage.Failed(testName, errorMessage, expectedText, actual)
    }
    else {
      TestMessage.Failed(testName, message, null, null)
    }
    pendingFailedTestMessage = StringBuilder()
    processor(testMessage)
  }

  private val TestMessage.Failed.diff: CheckResultDiff? get() =
    if (expected != null && actual != null) CheckResultDiff(expected, actual, message) else null

  companion object {
    private val TEST_FAILED_PATTERN: Pattern = Pattern.compile("((.+) )?expected: ?(.*) but was: ?(.*)",
                                                               Pattern.MULTILINE or Pattern.DOTALL)
  }

  sealed class TestMessage {
    class TextLine(val text: String): TestMessage()
    class Ok(val testName: String): TestMessage()
    class Failed(val testName: String, val message: String, val expected: String? = null, val actual: String? = null): TestMessage()
    class Congrats(val congratulations: String): TestMessage()
  }
}
