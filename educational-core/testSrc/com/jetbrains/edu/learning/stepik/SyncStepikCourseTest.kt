package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.SyncStepikCourseAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import com.jetbrains.edu.learning.testAction
import java.util.*

class SyncStepikCourseTest : EduTestCase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("remote_course.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      SECTIONS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("sections.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      UNITS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("units.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      LESSONS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("lessons.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      STEPS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("steps.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      SUBMISSIONS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("submissions.json")
    }
  }

  fun `test course updated isUpToDate == true`() {
    doTestCourseUpdated(true)
  }

  fun `test course updated isUpToDate == false`() {
    doTestCourseUpdated(false)
  }

  fun `test notification shown for up to date course`() {
    val course = createCourse(isCourseUpToDate = true, false, Date())

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("notification.course.up.to.date"), notification.title)
      }
    })

    myFixture.testAction(SyncStepikCourseAction.ACTION_ID)

    assertTrue("Course should be up to date", course.isUpToDate)
    assertTrue(notificationShown)
  }

  private fun doTestCourseUpdated(isUpToDate: Boolean) {
    val course = createCourse(isUpToDate)

    val submissionsManager = SubmissionsManager.getInstance(project)
    assertNull("SubmissionsManager should not contain submissions before submissions loading",
               submissionsManager.getSubmissionsFromMemory(setOf(1)))

    val expectedFileTree = fileTree {
      dir("newLesson") {
        dir("task1") {
          dir("src") {
            file("Task.java", "// type your solution here")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.java", "// type your solution here")
          }
          file("task.html")
        }
      }
    }

    myFixture.testAction(SyncStepikCourseAction.ACTION_ID)

    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    assertTrue("Updated course should be up to date", course.isUpToDate)

    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(1))
    check(submissions != null) { "Submissions list should not be null" }
    assertTrue(submissions.size == 1)
  }

  private fun createCourse(isCourseUpToDate: Boolean, hasItems: Boolean = true, courseUpdateDate: Date = Date(0)): EduCourse {
    val course = if (hasItems) {
      courseWithFiles(id = 1) {
        lesson("lesson1") {
          eduTask("task1", stepId = 1) {
            taskFile("src/Task.java")
            taskFile("task.html")
          }
        }
      }.asEduCourse().asRemote()
    }
    else {
      courseWithFiles(id = 1) { }.asEduCourse().asRemote()
    }
    course.apply {
      updateDate = courseUpdateDate
      isStepikPublic = true
      isUpToDate = isCourseUpToDate
    }
    return course
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/updateCourse/sync_action/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/courses?.*""".toRegex()
    private val SECTIONS_REQUEST_RE = """/api/sections?.*""".toRegex()
    private val UNITS_REQUEST_RE = """/api/units?.*""".toRegex()
    private val LESSONS_REQUEST_RE = """/api/lessons?.*""".toRegex()
    private val STEPS_REQUEST_RE = """/api/steps?.*""".toRegex()
    private val SUBMISSIONS_REQUEST_RE = """/api/submissions?.*""".toRegex()
  }
}