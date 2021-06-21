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

class HyperskillNextActivityTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  fun `test activity not available`() {
    createHyperskillProblemsProject()
    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(2)), content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  fun `test topic is completed`() {
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) { topic = 1 },
      StepMockResponse("/api/steps?topic=1&is_recommended=true", TheoryTask()) {
        topic = 1
        isCompleted = true
        id = 5
      }
    )

    openNextActivity(project, task)
    assertEquals(topicCompletedLink(1), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  fun `test next activity unsupported in IDE`() {
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) { topic = 1 },
      StepMockResponse("/api/steps?topic=1&is_recommended=true", ChoiceTask()) {
        topic = 1
        id = 5
      }
    )

    openNextActivity(project, task)
    assertEquals(stepLink(5), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  fun `test open next step in IDE`() {
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)
    val nextStepId = 5

    mockConnector.configureResponses(
      StepMockResponse("/api/steps?ids=2", task) { topic = 1 },
      StepMockResponse("/api/steps?ids=5", task) {
        topic = 1
        id = nextStepId
        topicTheory = 11
        block!!.name = CodeTask.CODE_TASK_TYPE
      },
      StepMockResponse("/api/steps?ids=11", TheoryTask()) {
        topic = 1
        title = "Test Topic Name"
        block!!.name = TheoryTask.THEORY_TASK_TYPE
      },
      StepMockResponse("/api/steps?topic=1&is_recommended=true", task) {
        block!!.name = CodeTask.CODE_TASK_TYPE
        topic = 1
        id = nextStepId
      }
    )

    (ProjectOpener.getInstance() as MockProjectOpener).project = project
    openNextActivity(project, task)
    assertEquals(nextStepId, findTask(0, 0, 2).id)
  }

  private fun createHyperskillProblemsProject() {
    hyperskillCourseWithFiles {
      section(HYPERSKILL_TOPICS) {
        lesson("Test Topic Name") {
          theoryTask("Theory", stepId = 1) {
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
}