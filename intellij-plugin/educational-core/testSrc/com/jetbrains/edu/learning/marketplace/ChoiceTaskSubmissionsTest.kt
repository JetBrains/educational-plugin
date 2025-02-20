package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.WRONG
import com.jetbrains.edu.learning.courseFormat.tasks.Task
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
import io.mockk.verify
import org.junit.Test
import retrofit2.Call

class ChoiceTaskSubmissionsTest : SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()
    loginFakeMarketplaceUser()
    mockJBAccount(testRootDisposable)
    CheckListener.EP_NAME.point.registerExtension(ChoiceTaskCheckListener(), testRootDisposable)
  }

  @Test
  fun `choice task submission test`() {
    courseWithFiles(
      courseProducer = ::EduCourse,
      id = 1
    ) {
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

    val task = findTask(0, 0) as ChoiceTask //getCourse().lessons[0].taskList[0]
    NavigationUtils.navigateToTask(project, task)
    task.selectedVariants.add(1)
    task.canCheckLocally = true
    testAction(CheckAction(task.getUICheckLabel()))
  }

  inner class ChoiceTaskCheckListener : CheckListener {
    private val connector: MarketplaceSubmissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    private val mockedService = mockk<SubmissionsService>()
    private val expectedSubmission = MarketplaceSubmission().apply {
      taskId = 1
      status = WRONG
    }
    private val slot = slot<MarketplaceSubmission>()

    override fun beforeCheck(project: Project, task: Task) {
      val mockedCall = mockk<Call<MarketplaceSubmission>>()
      every {
        mockedService.postSubmission(any(), any(), any(), any())
      } returns mockedCall

      every { connector["submissionsService"]() } returns mockedService

      every {
        connector["doPostSubmission"](
          project.course!!.id,
          task.id,
          capture(slot)
        )
      } answers {
        Result.success(slot.captured)
      }
    }

    override fun afterCheck(project: Project, task: Task, result: CheckResult) {
      verify(exactly = 1) { connector.postSubmission(any(), any(), any()) }
      val submission = slot.captured

      assertNotNull("Submission should be posted", submission)
      assertEquals(expectedSubmission.taskId, submission.taskId)
      assertEquals(expectedSubmission.status, submission.status)
    }
  }
}