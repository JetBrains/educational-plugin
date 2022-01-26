package com.jetbrains.edu.learning.checker.retry

import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.RetryAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.StepikUser
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.testAction
import org.apache.http.HttpStatus
import org.intellij.lang.annotations.Language
import java.util.*

class StepikRetryActionTest : SubmissionsTestBase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    EduSettings.getInstance().user = StepikUser.createEmptyUser().apply {
      userInfo = StepikUserInfo("Test User")
      userInfo.id = 1
    }
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        choiceTask(stepId = 5545,
                   name = "5545_choice_task",
                   isMultipleChoice = true,
                   choiceOptions = mapOf("2" to ChoiceOptionStatus.UNKNOWN,
                                         "1" to ChoiceOptionStatus.UNKNOWN,
                                         "0" to ChoiceOptionStatus.UNKNOWN),
                   status = CheckStatus.Failed) {
          taskFile("Task.txt", "")
        }
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    } as EduCourse
  }

  fun `test choice task correct`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> attemptList
          else -> error("Wrong path: ${path}")
        },
        HttpStatus.SC_CREATED
      )
    }
    val task = findTask(0, 0) as ChoiceTask
    NavigationUtils.navigateToTask(project, task)
    testAction(RetryAction.ACTION_ID)

    assertEquals("Name for ${task.name} doesn't match", "5545_choice_task", task.name)
    assertEquals("Status for ${task.name} doesn't match", CheckStatus.Unchecked, task.status)
    assertTrue("isMultipleChoice for ${task.name} doesn't match", task.isMultipleChoice)
    assertTrue("choiceOptions for ${task.name} doesn't match", task.selectedVariants.isEmpty())
    assertEquals("choiceOptions for ${task.name} doesn't match", mutableListOf("0", "1", "2"), task.choiceOptions.map { it.text })

  }

  fun `test edu task is not change in failed`() {
    NavigationUtils.navigateToTask(project, findTask(0, 1))
    testAction(RetryAction.ACTION_ID)
  }

  fun `test empty dataset`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts" -> emptyStep5454
          else -> error("Wrong path: ${path}")
        }
      )
    }

    val task = findTask(0, 0) as ChoiceTask
    NavigationUtils.navigateToTask(project, task)
    testAction(RetryAction.ACTION_ID)
    assertEquals("choiceOptions for ${task.name} doesn't match", mutableListOf("2", "1", "0"), task.choiceOptions.map { it.text })
  }

  @Language("JSON")
  private val attemptList = """{
  "attempts": [
    {
      "id": 74744589,
      "dataset": {
          "is_multiple_choice": true,
          "options": [ "0", "1", "2" ]
      },
      "dataset_url": null,
      "time": "${Date().format()}",
      "status": "active",
      "time_left": null,
      "step": 5545,
      "user": 1
    }
  ]
}
  """

  @Language("JSON")
  private val emptyStep5454 = """{
  "attempts": [
    {
      "id": 74744589,
      "dataset_url": null,
      "time": "${Date().format()}",
      "status": "active",
      "time_left": null,
      "step": 5545,
      "user": 1
    }
  ]
}
  """
}