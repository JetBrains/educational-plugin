package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.Assert

class CheckActionListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    checkStatus.invoke(task, result)
    if (expectedMessageForTask != null) {
      val expectedMessage = expectedMessageForTask!!.invoke(task)
      if (expectedMessage != null) {
        Assert.assertEquals("Checking output for " + getTaskName(task) + " fails", expectedMessage, result.message)
      }
      else {
        throw IllegalStateException(String.format("Unexpected task `%s`", task.name))
      }

    }
  }

  companion object {
    private val SHOULD_FAIL = fun(task: Task, result: CheckResult): Unit {
      val taskName = getTaskName(task)
      Assert.assertFalse("Check Task Action skipped for $taskName", result.status == CheckStatus.Unchecked)
      Assert.assertFalse("Check Task Action passed for $taskName", result.status == CheckStatus.Solved)
      println("Checking status for $taskName fails as expected")
      return Unit
    }

    private val SHOULD_PASS: Function2<Task, CheckResult, Unit> = fun(task: Task, result: CheckResult): Unit {
      val taskName = getTaskName(task)
      Assert.assertFalse("Check Task Action skipped for $taskName", result.status == CheckStatus.Unchecked)
      Assert.assertFalse("Check Task Action failed for $taskName", result.status == CheckStatus.Failed)
      println("Checking status for $taskName passed")
      return Unit
    }

    private fun getTaskName(task: Task): String {
      return task.lesson.name + "/" + task.name
    }

    // Those fields can be modified if some special checks are needed (return true if should run standard checks)
    private var checkStatus = SHOULD_PASS
    private var expectedMessageForTask: Function1<Task, String>? = null

    fun reset() {
      checkStatus = SHOULD_PASS
      expectedMessageForTask = null
    }

    fun shouldFail() {
      checkStatus = SHOULD_FAIL
    }

    fun expectedMessage(f: Function1<Task, String>) {
      expectedMessageForTask = f
    }
  }
}
