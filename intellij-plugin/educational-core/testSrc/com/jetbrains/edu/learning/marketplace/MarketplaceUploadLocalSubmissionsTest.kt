package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.SubmissionsTestBase
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class MarketplaceUploadLocalSubmissionsTest : SubmissionsTestBase() {

  private lateinit var helper: MockWebServerHelper
  private val requestBodies: MutableMap<String, MutableList<String>> = ConcurrentHashMap()

  override fun setUp() {
    super.setUp()
    helper = MockWebServerHelper(testRootDisposable)
    mockkObject(SubmissionsServiceHost.Companion)
    Disposer.register(testRootDisposable) {
      unmockkObject(SubmissionsServiceHost.Companion)
    }
    every { SubmissionsServiceHost.selectedHost } returns SelectedServiceHost(SubmissionsServiceHost.OTHER, helper.baseUrl)
    loginFakeMarketplaceUser()
    mockJBAccount(testRootDisposable)
    helper.addResponseHandlerWithRequestBodyRecording { _, path ->
      if (path.endsWith("/submission")) MockResponseFactory.fromString(SUBMISSION_RESPONSE) else MockResponseFactory.badRequest()
    }
  }

  @Test
  fun `test solved task is uploaded`() {
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")
    task.status = CheckStatus.Solved

    uploadLocalSubmissions(course)

    val submission = assertSinglePostedSubmission()
    assertEquals(task.id, submission.taskId)
    assertEquals(CheckStatus.Solved.rawStatus, submission.status)
  }

  @Test
  fun `test submission solution is built from local task state`() {
    val course = createEduCourse()

    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun ")
    }
    task.status = CheckStatus.Solved

    uploadLocalSubmissions(course)

    val submission = assertSinglePostedSubmission()
    assertTrue("Solution must contain the locally edited visible file", submission.solution.contains("fun foo() {}"))
    assertFalse("Invisible files must not be part of the uploaded solution", submission.solution.contains("hidden tests"))
  }

  @Test
  fun `test all solved framework lesson tasks are uploaded`() {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) { taskFile("src/Task.kt", "fun foo() {}") }
        eduTask("task2", stepId = 2) { taskFile("src/Task.kt", "fun foo() {}") }
        eduTask("task3", stepId = 3) { taskFile("src/Task.kt", "fun foo() {}") }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")
    val task3 = course.findTask("lesson1", "task3")
    val markers = mapOf(task1.id to "111", task2.id to "222", task3.id to "333")

    withVirtualFileListener(course) {
      for (task in listOf(task1, task2, task3)) {
        task.openTaskFileInEditor("src/Task.kt")
        myFixture.type(markers.getValue(task.id))
        task.status = CheckStatus.Solved
        if (task != task3) {
          testAction(NextTaskAction.ACTION_ID)
        }
      }
    }

    uploadLocalSubmissions(course)

    val posted = postedSubmissions()
    assertEquals(3, posted.size)
    for (task in listOf(task1, task2, task3)) {
      val submission = posted.find { it.taskId == task.id } ?: error("No submission uploaded for task ${task.name}")
      val marker = markers.getValue(task.id)
      assertTrue(submission.solution.contains(marker))
    }
  }

  @Test
  fun `test completed theory task is uploaded`() {
    val course = createCourseWithTheoryTask()
    val theoryTask = course.findTask("lesson1", "theory")
    theoryTask.status = CheckStatus.Solved

    uploadLocalSubmissions(course)

    val submission = assertSinglePostedSubmission()
    assertEquals(theoryTask.id, submission.taskId)
    assertEquals(CheckStatus.Solved.rawStatus, submission.status)
    assertEquals("Theory completion is posted as an empty submission", "", submission.solution)
  }

  @Test
  fun `test task with existing server submission is not re-uploaded`() {
    val course = createEduCourse()
    val task = course.findTask("lesson1", "task1")
    task.status = CheckStatus.Solved
    SubmissionsManager.getInstance(project).addToSubmissions(task.id, MarketplaceSubmission().apply {
      taskId = task.id
      id = Random.nextInt()
      status = CheckStatus.Solved.rawStatus
    })

    uploadLocalSubmissions(course)

    assertEmpty(postedSubmissions())
  }

  @Test
  fun `test unchecked task is not uploaded`() {
    val course = createEduCourse() // all tasks are unchecked
    uploadLocalSubmissions(course)
    assertEmpty(postedSubmissions())
  }

  @Test
  fun `test nothing is uploaded when course is not up to date`() {
    val course = createEduCourse()
    course.isUpToDate = false
    course.findTask("lesson1", "task1").status = CheckStatus.Solved

    uploadLocalSubmissions(course)

    assertEmpty(postedSubmissions())
  }

  private fun uploadLocalSubmissions(course: EduCourse) {
    runBlocking {
      MarketplaceSubmissionsConnector.getInstance().uploadLocalSubmissions(project, course)
    }
  }

  private fun postedSubmissions(): List<MarketplaceSubmission> {
    val mapper = MarketplaceSubmissionsConnector.getInstance().objectMapper
    return requestBodies.filterKeys { it.endsWith("/submission") }
      .values.flatten()
      .map { mapper.readValue<MarketplaceSubmission>(it) }
  }

  private fun assertSinglePostedSubmission(): MarketplaceSubmission {
    val posted = postedSubmissions()
    assertEquals(1, posted.size)
    return posted.single()
  }

  private fun MockWebServerHelper.addResponseHandlerWithRequestBodyRecording(handler: ResponseHandler) {
    addResponseHandler(testRootDisposable) { request, path ->
      requestBodies.getOrPut(path) { Collections.synchronizedList(mutableListOf()) } += request.body.readUtf8()
      handler(request, path)
    }
  }

  private fun createCourseWithTheoryTask(): EduCourse {
    return courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        theoryTask("theory", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse
  }

  private fun createEduCourse(): EduCourse = courseWithFiles(
    language = FakeGradleBasedLanguage,
    courseProducer = ::EduCourse,
    id = 1
  ) {
    lesson("lesson1") {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }.apply {
    isMarketplace = true
    marketplaceCourseVersion = 1
  } as EduCourse

  companion object {
    private const val SUBMISSION_RESPONSE = """{"id":5749695,"time":"2025-06-11T11:25:17.97224012"}"""
  }
}
