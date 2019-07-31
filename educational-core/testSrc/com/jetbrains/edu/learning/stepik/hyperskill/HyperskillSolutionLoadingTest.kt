package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.NavigationTestBase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.withTestDialog

class HyperskillSolutionLoadingTest : NavigationTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val result = SUBMISSION_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      when (stepId) {
        1 -> MockResponseFactory.fromFile(getTestFile("submissions_response_1.json"))
        2 -> MockResponseFactory.fromFile(getTestFile("submissions_response_2.json"))
        else -> MockResponseFactory.fromFile(getTestFile("submissions_response_empty.json"))
      }
    }
  }

  fun `test solution loading`() {
    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests1.kt", "fun tests1() {}")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test navigation after solution loading`() {
    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      withTestDialog(EduTestDialog(Messages.NO)) {
        task1.openTaskFileInEditor("src/Task.kt")
        myFixture.testAction(NextTaskAction())
      }
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }


  private fun createHyperskillCourse(): Course {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun tests2() {}")
        }
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests3.kt", "fun tests3() {}")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    return course
  }


  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  private fun getTestFile(fileName: String) = testDataPath + fileName

  companion object {
    private val SUBMISSION_REQUEST_RE = """/api/submission?.*step=(\d*).*""".toRegex()
  }
}
