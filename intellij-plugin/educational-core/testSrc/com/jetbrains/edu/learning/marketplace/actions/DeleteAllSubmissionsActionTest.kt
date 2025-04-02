package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.testAction
import io.mockk.coEvery
import org.junit.Test
import kotlin.random.Random

class DeleteAllSubmissionsActionTest : EduActionTestCase() {
  override fun setUp() {
    super.setUp()
    mockJBAccount(testRootDisposable)
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task1", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    val mockedService = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { mockedService.deleteAllSubmissions(any(), any(), any()) } returns true
  }

  /**
   * Not that meaningful since [SubmissionsManager] stores only submissions for current course
   */
  @Test
  fun `test delete all submissions`() {
    // setup
    val course = StudyTaskManager.getInstance(project).course as EduCourse
    val taskIds = course.allTasks.map { it.id }.toSet()
    for (taskId in taskIds) { // add one submission for each task
      SubmissionsManager.getInstance(project).addToSubmissions(taskId, generateMarketplaceSubmission())
    }
    val currentSubmissions = SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds.toSet())
    assertNotEmpty(currentSubmissions)
    assertEquals(taskIds.size, currentSubmissions.size)

    // when
    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.ALL
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    // then
    assertEmpty(
      SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds)
    )
  }

  @Test
  fun `test delete course submissions`() {
    // setup
    val course = StudyTaskManager.getInstance(project).course as EduCourse
    val taskIds = course.allTasks.map { it.id }.toSet()
    for (taskId in taskIds) { // add one submission for each task
      SubmissionsManager.getInstance(project).addToSubmissions(taskId, generateMarketplaceSubmission())
    }
    val currentSubmissions = SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds)
    assertNotEmpty(currentSubmissions)
    assertEquals(taskIds.size, currentSubmissions.size)

    // when
    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.COURSE
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    // then
    assertEmpty(
      SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds)
    )
  }

  @Test
  fun `test cancel deleting submissions`() {
    // setup
    val course = StudyTaskManager.getInstance(project).course as EduCourse
    val taskIds = course.allTasks.map { it.id }.toSet()
    for (taskId in taskIds) { // add one submission for each task
      SubmissionsManager.getInstance(project).addToSubmissions(taskId, generateMarketplaceSubmission())
    }
    val currentSubmissions = SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds.toSet())
    assertNotEmpty(currentSubmissions)
    assertEquals(taskIds.size, currentSubmissions.size)

    // when
    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.CANCEL
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    // then
    assertNotEmpty(
      SubmissionsManager.getInstance (project).getSubmissionsFromMemory(taskIds)
    )
    assertEquals(currentSubmissions.size, SubmissionsManager.getInstance(project).getSubmissionsFromMemory(taskIds).size)
  }

  private fun generateMarketplaceSubmission(): MarketplaceSubmission = MarketplaceSubmission().apply {
    id = Random.nextInt()
    solutionFiles = listOf(SolutionFile("file${id}", "text${id}", true))
    status = CheckStatus.values().random().toString()
  }
}