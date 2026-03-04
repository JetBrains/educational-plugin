package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FutureTracker
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.tracked
import com.jetbrains.edu.learning.ui.getUICheckLabel
import java.util.concurrent.CompletableFuture

abstract class SubmissionsTestBase : EduTestCase() {

  private val futureTracker = FutureTracker()

  protected fun doTestSubmissionsLoaded(taskIds: Set<Int>, taskIdsToSubmissionsNumber: Map<Int, Int>) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertEmpty(
      "SubmissionsManager should not contain submissions before submission loading",
      submissionsManager.getSubmissionsFromMemory(taskIds)
    )
    CompletableFuture.runAsync { submissionsManager.prepareSubmissionsContentWhenLoggedIn() }
      .thenApply {
        for (taskId in taskIds) {
          val submissionsNumber = taskIdsToSubmissionsNumber[taskId] ?: error("Number of submissions for taskId $taskId is null")
          checkSubmissionsPresent(submissionsManager, taskId, submissionsNumber)
        }
      }.tracked(futureTracker)
  }

  private fun checkSubmissionPresentWithStatus(submissionsManager: SubmissionsManager,
                                               taskId: Int,
                                               checkStatus: String) {
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotEmpty(submissions)
    assertEquals(1, submissions.size)
    assertEquals(checkStatus, submissions[0].status)
  }

  protected fun checkSubmissionsPresent(submissionsManager: SubmissionsManager,
                                        taskId: Int,
                                        submissionsNumber: Int = 1) {
    submissionsManager.futureTracker.waitForPendingFuturesInTests()
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotEmpty(submissions)
    assertTrue(submissions.size == submissionsNumber)
  }

  protected fun doTestSubmissionAddedAfterTaskCheck(taskId: Int, checkStatus: String) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertEmpty("SubmissionsManager should not contain submissions before task check",
               submissionsManager.getSubmissionsFromMemory(setOf(taskId)))

    checkTask()
    checkSubmissionPresentWithStatus(submissionsManager, taskId, checkStatus)
  }

  private fun checkTask(lessonIndex: Int = 0, taskIndex: Int = 0) {
    val task = findTask(lessonIndex, taskIndex)
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }

  override fun tearDown() {
    try {
      futureTracker.waitForPendingFuturesInTests()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
