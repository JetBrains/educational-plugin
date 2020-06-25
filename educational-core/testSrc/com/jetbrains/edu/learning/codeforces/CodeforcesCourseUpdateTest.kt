package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.File

class CodeforcesCourseUpdateTest : CodeforcesTestCase() {
  private val expectedTaskADescription: String by lazy {
    FileUtil.loadFile(File(testDataPath, expectedTaskDescriptionFiles.getValue(1211).getValue("A")))
  }
  private val mockConnector: MockCodeforcesConnector get() = CodeforcesConnector.getInstance() as MockCodeforcesConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) {
      MockResponseFactory.fromFile(File(testDataPath, contest1211).path)
    }
  }

  override fun tearDown() {
    try {
      val updateChecker = CodeforcesCourseUpdateChecker.getInstance(project)
      updateChecker.invocationNumber = 0
      updateChecker.cancelCheckRequests()
    }
    finally {
      super.tearDown()
    }
  }

  fun `test outdated description`() {
    val course = createCodeforcesCourse("OUTDATED")
    CodeforcesCourseUpdateChecker.getInstance(project).check()
    assertEquals(expectedTaskADescription, course.allTasks.first().descriptionText)
  }

  fun `test up-to-date description`() {
    val course = createCodeforcesCourse()
    CodeforcesCourseUpdateChecker.getInstance(project).check()
    assertEquals(expectedTaskADescription, course.allTasks.first().descriptionText)
  }

  private fun createCodeforcesCourse(taskADescription: String? = null): CodeforcesCourse {
    val course = courseWithFiles(
      courseProducer = ::CodeforcesCourse
    ) {
      lesson(CodeforcesNames.CODEFORCES_PROBLEMS) {
        codeforcesTask("taskName", taskADescription ?: expectedTaskADescription)
      }
    } as CodeforcesCourse

    StudyTaskManager.getInstance(project).course = course
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)
    return course
  }
}