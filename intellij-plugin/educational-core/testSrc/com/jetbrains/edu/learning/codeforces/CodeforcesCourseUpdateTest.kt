package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.junit.Test
import java.io.File

class CodeforcesCourseUpdateTest : CodeforcesTestCase() {
  private val expectedTaskADescription: String by lazy {
    loadText(expectedTaskDescriptionFiles.getValue(1211).getValue('A'))
  }
  private val mockConnector: MockCodeforcesConnector get() = CodeforcesConnector.getInstance() as MockCodeforcesConnector

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ ->
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

  @Test
  fun `test outdated description`() {
    val course = createCodeforcesCourse("OUTDATED")
    CodeforcesCourseUpdateChecker.getInstance(project).check()
    val codeforcesTask = course.allTasks.first() as CodeforcesTask
    codeforcesTask.checkTaskDescription(1211, 'A')
  }

  @Test
  fun `test up-to-date description`() {
    val course = createCodeforcesCourse()
    CodeforcesCourseUpdateChecker.getInstance(project).check()
    val codeforcesTask = course.allTasks.first() as CodeforcesTask
    codeforcesTask.checkTaskDescription(1211, 'A')
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
    CodeforcesCourseUpdateChecker.getInstance(project).course = course
    return course
  }
}