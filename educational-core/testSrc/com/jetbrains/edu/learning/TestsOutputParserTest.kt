package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.checker.CheckUtils.*
import com.jetbrains.edu.learning.checker.TestsOutputParser.getCheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import junit.framework.TestCase

class TestsOutputParserTest : TestCase() {
  fun `test failure message`() {
    val failedMessage = "your test failed"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_FAILED $failedMessage"), false)
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals(failedMessage, checkResult.message.trim())
  }

  fun `test multiline failure message`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_FAILED $failedLine1", failedLine2), false)
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test multiline failure message with success in the middle`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            failedLine2,
                                            "$STUDY_PREFIX $TEST_OK",
                                            "$STUDY_PREFIX $TEST_FAILED $failedLine1"), false)
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test multiline failure message with congrats in the middle`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            failedLine2,
                                            "$STUDY_PREFIX $CONGRATS_MESSAGE",
                                            "$STUDY_PREFIX $TEST_FAILED $failedLine1"), false)
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test multiline failure`() {
    val failedLine1 = "123"
    val failedLine2 = "456"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_FAILED $failedLine1",
                                            "$STUDY_PREFIX $failedLine2"), false)
    assertEquals(CheckStatus.Failed, checkResult.status)
    assertEquals("$failedLine1\n$failedLine2", checkResult.message.trim())
  }

  fun `test success`() {
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $TEST_OK"), false)
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(CONGRATULATIONS, checkResult.message.trim())
  }

  fun `test custom congrats message`() {
    val congrats = "Yoo-hoo!"
    val checkResult = getCheckResult(listOf("$STUDY_PREFIX $CONGRATS_MESSAGE $congrats"), false)
    assertEquals(CheckStatus.Solved, checkResult.status)
    assertEquals(congrats, checkResult.message.trim())
  }
}
