package com.jetbrains.edu.learning.stepik

import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.testAction

abstract class SubmissionsTestBase : EduTestCase() {

  protected fun doTestSubmissionsLoaded(taskIds: Set<Int>, taskIdsToSubmissionsNumber: Map<Int, Int>) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertNull("SubmissionsManager should not contain submissions before submissions loading",
               submissionsManager.getSubmissionsFromMemory(taskIds))
    submissionsManager.prepareSubmissionsContent()

    for (taskId in taskIds) {
      val submissionsNumber = taskIdsToSubmissionsNumber[taskId] ?: error("Submissions number for taskId ${taskId} is null")
      checkSubmissionsPresent(submissionsManager, taskId, submissionsNumber)
    }
  }

  private fun checkSubmissionPresentWithStatus(submissionsManager: SubmissionsManager,
                                               taskId: Int,
                                               checkStatus: String) {
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotNull("Submissions list should not be null", submissions)
    assertEquals(1, submissions!!.size)
    assertEquals(checkStatus, submissions[0].status)
  }

  private fun checkSubmissionsPresent(submissionsManager: SubmissionsManager,
                                      taskId: Int,
                                      submissionsNumber: Int = 1) {
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotNull("Submissions list should not be null", submissions)
    assertTrue(submissions!!.size == submissionsNumber)
  }

  protected fun doTestSubmissionAddedAfterTaskCheck(taskId: Int, checkStatus: String) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertNull("SubmissionsManager should not contain submissions before task check",
               submissionsManager.getSubmissionsFromMemory(setOf(taskId)))

    NavigationUtils.navigateToTask(project, findTask(0, 0))
    testAction(CheckAction.ACTION_ID)

    checkSubmissionPresentWithStatus(submissionsManager, taskId, checkStatus)
  }
}
