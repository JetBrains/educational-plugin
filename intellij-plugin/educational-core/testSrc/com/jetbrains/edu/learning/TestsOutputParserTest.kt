package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.checker.TestsOutputParser.Companion.CONGRATS_MESSAGE
import com.jetbrains.edu.learning.checker.TestsOutputParser.Companion.STUDY_PREFIX
import com.jetbrains.edu.learning.checker.TestsOutputParser.Companion.TEST_FAILED
import com.jetbrains.edu.learning.checker.TestsOutputParser.Companion.TEST_OK
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.junit.Assert.assertEquals
import org.junit.Test

class TestsOutputParserTest {
  @Test
  fun `test failure message`() {
    val failedMessage = "your test failed"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedMessage")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals(failedMessage, checkResult.message.trim())
  }

  @Test
  fun `test multiline failure message with success in the middle`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            "$STUDY_PREFIX $failedLine2",
                                            "$STUDY_PREFIX $TEST_OK",
                                            "$STUDY_PREFIX $TEST_FAILED $failedLine1")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  @Test
  fun `test multiline failure message with congrats in the middle`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            "$STUDY_PREFIX $failedLine2",
                                            "$STUDY_PREFIX $CONGRATS_MESSAGE",
                                            "$STUDY_PREFIX $TEST_FAILED $failedLine1")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  @Test
  fun `test multiline failure`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            "$STUDY_PREFIX $failedLine2")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  @Test
  fun `test multiline failure with new line symbols at the end`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1\n",
                                     "$STUDY_PREFIX $failedLine2\n")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  @Test
  fun `test success`() {
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_OK")
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(CONGRATULATIONS, checkResult.message.trim())
  }

  @Test
  fun `test custom congrats message`() {
    val congrats = "Yoo-hoo!"
    val checkResult = getCheckResult("$STUDY_PREFIX $CONGRATS_MESSAGE $congrats")
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(congrats, checkResult.message.trim())
  }

  @Test
  fun `test failure message with diff`() {
    val failedMessage = "expected: A but was: B"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedMessage")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("A", checkResult.diff?.expected)
    assertEquals("B", checkResult.diff?.actual)
    assertEquals(EduCoreBundle.message("check.incorrect"), checkResult.message.trim())
  }

  private fun getCheckResult(vararg lines: String): CheckResult = TestsOutputParser().getCheckResult(listOf(*lines), false)
}
