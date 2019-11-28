package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATS_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.checker.CheckUtils.STUDY_PREFIX
import com.jetbrains.edu.learning.checker.CheckUtils.TEST_FAILED
import com.jetbrains.edu.learning.checker.CheckUtils.TEST_OK
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import junit.framework.TestCase

class TestsOutputParserTest : TestCase() {
  fun `test failure message`() {
    val failedMessage = "your test failed"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedMessage")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals(failedMessage, checkResult.message.trim())
  }

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

  fun `test multiline failure`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            "$STUDY_PREFIX $failedLine2")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test multiline failure with new line symbols at the end`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_FAILED $failedLine1\n",
                                     "$STUDY_PREFIX $failedLine2\n")
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test success`() {
    val checkResult = getCheckResult("$STUDY_PREFIX $TEST_OK")
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(CONGRATULATIONS, checkResult.message.trim())
  }

  fun `test custom congrats message`() {
    val congrats = "Yoo-hoo!"
    val checkResult = getCheckResult("$STUDY_PREFIX $CONGRATS_MESSAGE $congrats")
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(congrats, checkResult.message.trim())
  }
  
  private fun getCheckResult(vararg lines: String): CheckResult = TestsOutputParser().getCheckResult(listOf(*lines), false)
}
