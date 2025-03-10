package com.jetbrains.edu.learning.marketplace

import com.intellij.testFramework.PlatformTestUtil.waitWhileBusy
import com.intellij.util.application
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.WRONG
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.SubmissionsService
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test

class ChoiceTaskSubmissionsTest : SubmissionsTestBase() {
  private val wrongSubmission = MarketplaceSubmission().apply {
    taskId = 1
    status = WRONG
    solution = "[{\"name\":\"text.txt\",\"placeholders\":[],\"is_visible\":true,\"text\":\"\"}]"
  }
  private val correctSubmission = MarketplaceSubmission().apply {
    taskId = 1
    status = CORRECT
    solution = "[{\"name\":\"text.txt\",\"placeholders\":[],\"is_visible\":true,\"text\":\"\"}]"
  }

  private val slot = slot<MarketplaceSubmission>()

  override fun setUp() {
    super.setUp()
    mockJBAccount(testRootDisposable)

    val connector = mockService<MarketplaceSubmissionsConnector>(application)
    val service = mockk<SubmissionsService>()
    every { connector["submissionsService"]() } returns service

    every {
      connector["doPostSubmission"](1, 1, capture(slot))
    } answers { Result.success(slot.captured) }
  }

  @Test
  fun `correct choice task submission test`() {
    courseWithFiles(id = 1) {
      lesson("lesson1") {
        choiceTask("task1", stepId = 1, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
        choiceTask(
          "task2",
          stepId = 2,
          choiceOptions = mapOf("option" to ChoiceOptionStatus.CORRECT, "another option" to ChoiceOptionStatus.INCORRECT)
        ) {
          taskFile("text.txt")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    val task = findTask(0, 0) as ChoiceTask
    NavigationUtils.navigateToTask(project, task)
    task.selectedVariants.add(0)
    task.canCheckLocally = true
    testAction(CheckAction(task.getUICheckLabel()))

    waitWhileBusy { !slot.isCaptured }
    val submission = slot.captured

    assertNotNull("Submission should be posted", submission)
    assertEquals(correctSubmission.taskId, submission.taskId)
    assertEquals(correctSubmission.status, submission.status)
    assertEquals(correctSubmission.solution, submission.solution)
  }

  @Test
  fun `wrong choice task submission test`() {
    courseWithFiles(id = 1) {
      lesson("lesson1") {
        choiceTask("task1", stepId = 1, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("text.txt")
        }
        choiceTask(
          "task2",
          stepId = 2,
          choiceOptions = mapOf("option" to ChoiceOptionStatus.CORRECT, "another option" to ChoiceOptionStatus.INCORRECT)
        ) {
          taskFile("text.txt")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    val task = findTask(0, 0) as ChoiceTask
    NavigationUtils.navigateToTask(project, task)
    task.selectedVariants.add(1)
    task.canCheckLocally = true
    testAction(CheckAction(task.getUICheckLabel()))

    waitWhileBusy { !slot.isCaptured }
    val submission = slot.captured

    assertNotNull("Submission should be posted", submission)
    assertEquals(wrongSubmission.taskId, submission.taskId)
    assertEquals(wrongSubmission.status, submission.status)
    assertEquals(correctSubmission.solution, submission.solution)
  }
}