package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import org.junit.Test

class HyperskillNextActivityTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test activity not available`() {
    val task = initProject()
    configureResponsesForCurrentTask(task)
    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(codeStepFromInitialProject.id)),
                   content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  @Test
  fun `test no topic for a task`() {
    initProject()
    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(codeStepFromInitialProject.id)),
                   content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  @Test
  fun `test activity not available without login`() {
    logOutFakeHyperskillUser()
    createHyperskillProblemsProject()

    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content"), content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  @Test
  fun `test topic is completed`() {
    val task = initProject()

    configureResponsesForCurrentTask(task)
    configureResponseForTopicSteps()

    openNextActivity(project, task)
    assertEquals(topicCompletedLink(DEFAULT_TOPIC_ID), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  @Test
  fun `test next activity unsupported in IDE`() {
    val task = initProject()

    val nextStepId = 3
    val nextStep = StepInfo(nextStepId, "Test Matching Task")

    configureResponsesForCurrentTask(task)
    configureResponseForNextStep(task, nextStep)
    configureResponseForTopicSteps()

    (ProjectOpener.getInstance() as MockProjectOpener).project = project
    openNextActivity(project, task)
    assertEquals(stepLink(3), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
    assertEquals(nextStepId, findTask(0, 0, 2).id)
  }

  @Test
  fun `test open next step in IDE`() {
    val task = initProject()

    // mock responses setup
    val nextStepId = 5
    val nextStep = StepInfo(nextStepId, "Test Code Task 2")

    configureResponsesForCurrentTask(task)
    configureResponseForNextStep(task, nextStep)
    configureResponseForTopicSteps()

    // test itself
    (ProjectOpener.getInstance() as MockProjectOpener).project = project
    openNextActivity(project, task)
    assertEquals(nextStepId, findTask(0, 0, 3).id)
  }

  @Test
  fun `test next step is the same as current`() {
    val task = initProject()

    configureResponsesForCurrentTask(task)
    configureResponseForTopicSteps()

    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertEquals(true, shown)
      assertEquals(EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(codeStepFromInitialProject.id)),
                   content)
    }) {
      openNextActivity(project, findTask(0, 0, 1))
    }
  }

  private fun initProject(): Task {
    createHyperskillProblemsProject()
    val task = findTask(0, 0, 1)
    return task
  }

  private fun createHyperskillProblemsProject() {
    hyperskillCourseWithFiles {
      section(HYPERSKILL_TOPICS) {
        lesson(theoryStepFromInitialProject.title) {
          theoryTask("Theory", stepId = theoryStepFromInitialProject.id) {
            taskFile("Task.txt")
          }
          codeTask(codeStepFromInitialProject.title, stepId = codeStepFromInitialProject.id) {
            taskFile("Task.txt")
          }
        }
      }
    }
  }

  private fun configureResponseForTopicSteps() {
    val topicInfo = TopicInfo(DEFAULT_TOPIC_ID, topicResponseFileName)
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      if (request.pathWithoutPrams.endsWith(topicInfo.path)) {
        mockResponse(topicInfo.file)
      }
      else {
        null
      }
    }
  }

  private fun configureResponsesForCurrentTask(task: Task) {
    mockConnector.configureResponses(
      StepMockResponse(theoryStepFromInitialProject.urlWithPrams, TheoryTask()) {
        block!!.name = TheoryTask.THEORY_TASK_TYPE
        topic = DEFAULT_TOPIC_ID
        id = theoryStepFromInitialProject.id
        topicTheory = theoryStepFromInitialProject.id
        title = theoryStepFromInitialProject.title
        isRecommended = true
      },
      StepMockResponse(codeStepFromInitialProject.urlWithPrams, task) { topic = DEFAULT_TOPIC_ID }
    )
  }

  private fun configureResponseForNextStep(task: Task, nextStep: StepInfo) {
    mockConnector.configureResponses(
      StepMockResponse(nextStep.urlWithPrams, task) {
        block!!.name = CodeTask.CODE_TASK_TYPE
        topic = DEFAULT_TOPIC_ID
        id = nextStep.id
        topicTheory = theoryStepFromInitialProject.id
        title = nextStep.title
        isRecommended = true
      }
    )
  }

  private data class StepMockResponse(val path: String, val task: Task, val initStepSource: HyperskillStepSource.() -> Unit)

  private fun MockHyperskillConnector.configureResponses(vararg responses: StepMockResponse) {
    withResponseHandler(testRootDisposable) { request, _ ->
      responses.find { request.pathWithoutPrams == it.path }?.let {
        mockResponseFromTask(it.task, it.initStepSource)
      }
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/nextActivity/"

  private val topicResponseFileName: String
    get() = "${getTestName(true).trim().replace(" ", "_")}.json"

  companion object {
    private const val DEFAULT_TOPIC_ID = 1

    private val theoryStepFromInitialProject = StepInfo(1, "Test topic name")
    private val codeStepFromInitialProject = StepInfo(2, "Test Code Task")
  }
}