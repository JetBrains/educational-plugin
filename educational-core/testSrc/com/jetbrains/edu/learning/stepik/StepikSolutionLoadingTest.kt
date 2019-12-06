package com.jetbrains.edu.learning.stepik

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils.getFirstTask
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import junit.framework.TestCase
import okhttp3.mockwebserver.MockResponse
import java.text.SimpleDateFormat

class StepikSolutionLoadingTest : NavigationTestBase() {

  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  private fun configureSubmissionsResponse(items: Map<Int, String>) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      PROGRESSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("progresses.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val result = SUBMISSIONS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      items[stepId]?.let { mockResponse(it) } ?: mockResponse("submissions_response_empty.json")
    }
  }

  fun `test task statuses`() {
    configureSubmissionsResponse(mapOf(1 to "submissions_response_1.json", 2 to "submissions_response_2.json"))
    val course = createStepikCourse()

    StepikSolutionsLoader.getInstance(project).loadSolutions(null, course)
    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Failed, CheckStatus.Solved))
  }

  fun `test do not set expired task status and solution`() {
    configureSubmissionsResponse(mapOf(1 to "submissions_response_expired.json", 2 to "submissions_response_2.json"))
    val course = createStepikCourse()

    StepikSolutionsLoader.getInstance(project).loadSolutions(null, course)

    fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", """
            fun foo() {}
          """)
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Unchecked, CheckStatus.Solved))
  }

  fun `test tasks to update`() {
    configureSubmissionsResponse(mapOf(1 to "submissions_response_1.json", 2 to "submissions_response_2.json"))
    val course = createStepikCourse()

    val tasksToUpdate = StepikSolutionsLoader.getInstance(project).tasksToUpdate(course)
    assertEquals("Unexpected number of tasks", 2, tasksToUpdate.size)
    val firstTaskToUpdate = tasksToUpdate[0]
    val secondTaskToUpdate = tasksToUpdate[1]
    checkTaskStatuses(course.allTasks, listOf(firstTaskToUpdate.status, secondTaskToUpdate.status))
    assertEquals(2, firstTaskToUpdate.taskFiles.size)
    assertEquals(2, secondTaskToUpdate.taskFiles.size)
  }

  fun `test do not update tasks with expired submissions`() {
    configureSubmissionsResponse(
      mapOf(1 to "submissions_response_expired.json", 2 to "submissions_response_2.json"))
    val course = createStepikCourse()
    val tasksToUpdate = StepikSolutionsLoader.getInstance(project).tasksToUpdate(course)
    assertEquals("Unexpected number of tasks", 1, tasksToUpdate.size)
    val taskToUpdate = tasksToUpdate[0]
    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Unchecked, taskToUpdate.status))
    assertEquals(2, taskToUpdate.taskFiles.size)
  }

  fun `test framework lesson solutions`() {
    configureSubmissionsResponse(
      mapOf(1 to "submissions_response_1.json", 2 to "submissions_response_2.json"))
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("task.kt", "fun fizz() {}")
        }
        eduTask("task2", stepId = 2) {
          taskFile("task.kt", "fun fizz() {}")
        }
      }
    }
    StepikSolutionsLoader.getInstance(project).loadSolutions(null, course)
    UIUtil.dispatchAllInvocationEvents()

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("task.kt", """
            // comment from task 1
            fun fizz() {}
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir, myFixture)

    getFirstTask(course)!!.openTaskFileInEditor("task.kt")
    myFixture.testAction(NextTaskAction())

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("task.kt", """
            // comment from task 2
            fun fizz() {}
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Failed, CheckStatus.Solved))
  }

  fun `test framework lesson solutions with dependencies`() {
    configureSubmissionsResponse(
      mapOf(1 to "submissions_response_with_placeholders_1.json", 2 to "submissions_response_with_placeholders_2.json"))
    val course = courseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("fizz.kt", "fun fizz() = <p>\"Fizz\"</p>") {
            placeholder(0, placeholderText = "TODO()")
          }
        }
        eduTask("task2", stepId = 2) {
          taskFile("foo.kt", "fun foo() = <p>\"Foo\"</p>") {
            placeholder(0, placeholderText = "TODO()", dependency = "lesson1#task1#fizz.kt#1")
          }
        }
      }
    }

    StepikSolutionsLoader.getInstance(project).loadSolutions(null, course)

    UIUtil.dispatchAllInvocationEvents()

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            // comment from task 1
            fun fizz() = "Fizz"
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(rootDir, myFixture)

    getFirstTask(course)!!.openTaskFileInEditor("fizz.kt")
    myFixture.testAction(NextTaskAction())

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("foo.kt", """
          // comment from task 2
          fun foo() = "Foo"
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Failed, CheckStatus.Solved))
  }

  private fun mockResponse(fileName: String): MockResponse = MockResponseFactory.fromFile(getTestFile(fileName))

  private fun createStepikCourse(): EduCourse {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse
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
    } as EduCourse
    setUpdateDate(course)
    return course
  }

  private fun setUpdateDate(course: EduCourse) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val updateDate = formatter.parse("1970-01-02 07:24:15")
    course.updateDate = updateDate
    for (task in course.allTasks) {
      task.updateDate = updateDate
    }
  }

  private fun checkTaskStatuses(tasks: List<Task>, expectedStatuses: List<CheckStatus>) {
    tasks.zip(expectedStatuses) { task, status -> TestCase.assertEquals(status, task.status) }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/loadSolutions/"

  private fun getTestFile(fileName: String) = testDataPath + fileName

  override fun tearDown() {
    SubmissionsManager.clear()
    super.tearDown()
  }

  companion object {
    private val SUBMISSIONS_REQUEST_RE = """/api/submissions?.*step=(\d*).*""".toRegex()
    private val PROGRESSES_REQUEST_RE = """/api/progresses?.*""".toRegex()
  }
}