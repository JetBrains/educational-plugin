package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillNextActivityTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  fun `test activity not available`() {
    loginFakeUser()
    createHyperskillProblemsProject()
    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(2)), content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  fun `test activity not available without login`() {
    HyperskillSettings.INSTANCE.account = null
    createHyperskillProblemsProject()
    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content"), content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  fun `test topic is completed`() {
    loginFakeUser()
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) { topic = 1 },
      StepMockResponse("/api/steps?topic=1", TheoryTask()) {
        topic = 1
        isCompleted = true
        id = 5
        block!!.name = TheoryTask.THEORY_TASK_TYPE
      }
    )

    openNextActivity(project, task)
    assertEquals(topicCompletedLink(1), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  fun `test next activity unsupported in IDE`() {
    loginFakeUser()
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) { topic = 1 },
      StepMockResponse("/api/steps?topic=1", ChoiceTask()) {
        topic = 1
        id = 5
        block!!.name = ChoiceTask.CHOICE_TASK_TYPE
      }
    )

    openNextActivity(project, task)
    assertEquals(stepLink(5), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  fun `test open next step in IDE`() {
    loginFakeUser()
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)
    val nextStepId = 5

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) {
        topic = 1
        id = 2
        topicTheory = 11
        title = "Test Code Task"
      },
      StepMockResponse("/api/steps?ids=5", task) {
        block!!.name = CodeTask.CODE_TASK_TYPE
        topic = 1
        id = nextStepId
        topicTheory = 11
        title = "Test Code Task 2"
        isRecommended = true
      },
      StepMockResponse("/api/steps?ids=11", TheoryTask()) {
        block!!.name = TheoryTask.THEORY_TASK_TYPE
        topic = 1
        id = 11
        topicTheory = 11
        title = "Test Topic Name"
        isRecommended = true
      }
    )
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      if (request.path.endsWith("/api/steps?topic=1")) {
        mockResponse("steps_1_topic_response.json")
      }
      else null
    }

    (ProjectOpener.getInstance() as MockProjectOpener).project = project
    openNextActivity(project, task)
    assertEquals(nextStepId, findTask(0, 0, 2).id)
  }

  private fun createHyperskillProblemsProject() {
    hyperskillCourseWithFiles {
      section(HYPERSKILL_TOPICS) {
        lesson("Test Topic Name") {
          theoryTask("Theory", stepId = 11) {
            taskFile("Task.txt")
          }
          codeTask("Test Code Task", stepId = 2) {
            taskFile("Task.txt")
          }
        }
      }
    }
  }

  private data class StepMockResponse(val path: String, val task: Task, val initStepSource: HyperskillStepSource.() -> Unit)

  private fun MockHyperskillConnector.configureResponses(vararg responses: StepMockResponse) {
    withResponseHandler(testRootDisposable) { request ->
      responses.find { request.path == it.path }?.let {
        mockResponseFromTask(it.task, it.initStepSource)
      }
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"
}