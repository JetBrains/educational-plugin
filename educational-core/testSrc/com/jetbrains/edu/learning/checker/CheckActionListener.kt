package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

class CheckActionListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    checkResultVerifier(task, result)
    val messageProducer = expectedMessageProducer ?: return
    val expectedMessage = messageProducer(task) ?: error("Unexpected task `${task.name}`")
    assertEquals("Checking output for ${getTaskName(task)} fails", expectedMessage, result.message)
  }

  companion object {
    private val SHOULD_FAIL: (Task, CheckResult) -> Unit = { task, result ->
      val taskName = getTaskName(task)
      assertFalse("Check Task Action skipped for $taskName", result.status == CheckStatus.Unchecked)
      assertFalse("Check Task Action passed for $taskName", result.status == CheckStatus.Solved)
      println("Checking status for $taskName: fails as expected")
    }

    private val SHOULD_PASS: (Task, CheckResult) -> Unit = { task, result ->
      val taskName = getTaskName(task)
      assertFalse("Check Task Action skipped for $taskName", result.status == CheckStatus.Unchecked)
      assertFalse("Check Task Action failed for $taskName", result.status == CheckStatus.Failed)
      println("Checking status for $taskName: passes as expected")
    }

    private fun getTaskName(task: Task): String = "${task.lesson.name}/${task.name}"

    // Those fields can be modified if some special checks are needed (return true if should run standard checks)
    private var checkResultVerifier = SHOULD_PASS
    private var expectedMessageProducer: ((Task) -> String?)? = null

    @JvmStatic
    fun reset() {
      setCheckResultVerifier(SHOULD_PASS)
      expectedMessageProducer = null
    }

    @JvmStatic
    fun shouldFail() {
      setCheckResultVerifier(SHOULD_FAIL)
    }

    @JvmStatic
    fun expectedMessage(producer: (Task) -> String?) {
      expectedMessageProducer = producer
    }

    @JvmStatic
    fun setCheckResultVerifier(verifier: (Task, CheckResult) -> Unit) {
      checkResultVerifier = verifier
    }
  }
}
