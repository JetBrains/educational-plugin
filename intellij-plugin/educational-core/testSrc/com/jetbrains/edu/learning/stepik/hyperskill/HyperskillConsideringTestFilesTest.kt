package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillConsideringTestFilesTest : NavigationTestBase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  @Test
  fun `test files in project`() {
    val course = createHyperskillCourse()
    doTestFiles(course)
  }

  @Test
  fun `test files in topic section`() {
    val course = hyperskillCourse {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson") {
          eduTask("edu task") {
            taskFile(TASK_FILE_NAME)
            taskFile("some_random_test_file_name.json", visible = false)
          }
          codeTask("code task") {
            taskFile(TASK_FILE_NAME)
            taskFile("another_random_test_file_name.avi", visible = false)
          }
        }
      }
    }
    doTestFiles(course)
  }

  private fun doTestFiles(course: HyperskillCourse) {
    val configurator = course.configurator!!

    course.allTasks.forEach { task ->
      val taskFiles = task.taskFiles.filter { it.value.isVisible }.values
      assertTrue(taskFiles.all { file -> !configurator.isTestFile(task, file.name) })

      val testFiles = task.taskFiles.filter { !it.value.isVisible }.values
      assertTrue(testFiles.all { file -> configurator.isTestFile(task, file.name) })
    }
  }

  @Test
  fun `test propagate user changes to next task`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor(TASK_FILE_NAME)
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", FIRST_TASK_FILE_TEXT)
          }
          file(SECOND_TEST_FILE_NAME)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test do not propagate user changes to prev task (next, prev)`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor(TASK_FILE_NAME)
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor(TASK_FILE_NAME)
      testAction(PreviousTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", FIRST_TASK_FILE_TEXT)
          }
          dir(EduNames.TEST) {
            file(USER_VISIBLE_TEST_FILE_NAME)
          }
          file(FIRST_TEST_FILE_NAME)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading for non-current task with non-visible files`() {
    logInFakeHyperskillUser()
    mockConnector.withResponseHandler(testRootDisposable) { _, _ -> MockResponseFactory.fromString(submissions) }

    val course = createHyperskillCourse()
    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor(TASK_FILE_NAME)
      testAction(NextTaskAction.ACTION_ID)

      task2.openTaskFileInEditor(TASK_FILE_NAME) // open second task
      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course) // load submission for the first (non-current) task

      testAction(PreviousTaskAction.ACTION_ID)  // go back to the first one
    }

    val configurator = course.configurator ?: error("Configurator must be specified")
    assertEquals(false, configurator.isTestFile(task1, TASK_FILE_NAME))
    assertEquals(true, configurator.isTestFile(task1, "${EduNames.TEST}/$USER_VISIBLE_TEST_FILE_NAME"))
    assertEquals(true, configurator.isTestFile(task1, FIRST_TEST_FILE_NAME))

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            // Updated
            file("Task.kt", "fun userFoo() {}")
          }
          dir(EduNames.TEST) {
            // Not updated because file inside EduNames.TEST and this folder is specified at
            // com.jetbrains.edu.learning.configurators.FakeGradleConfigurator.getTestDirs
            file(USER_VISIBLE_TEST_FILE_NAME, "fun myTest() {}")
          }
          // Not updated
          file(FIRST_TEST_FILE_NAME, "test")
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  private fun createHyperskillCourse(): HyperskillCourse {
    return hyperskillCourseWithFiles(completeStages = true) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile(TASK_FILE_NAME, FIRST_TASK_FILE_TEXT)
          taskFile(FIRST_TEST_FILE_NAME, "test", visible = false)
          dir(EduNames.TEST) {
            taskFile(USER_VISIBLE_TEST_FILE_NAME, "fun myTest() {}")
          }
        }
        eduTask("task2", stepId = 2) {
          taskFile(TASK_FILE_NAME, SECOND_TASK_FILE_TEXT)
          taskFile(SECOND_TEST_FILE_NAME, "test", visible = false)
        }
      }
    }
  }

  @Language("JSON")
  private val submissions = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": 503037,
          "can_download_test_set": false,
          "eta": 0,
          "feedback": {
            "message": "Congratulations!"
          },
          "hint": "Congratulations!",
          "id": 1457792,
          "next_test_available_at": null,
          "reply": {
            "choices": null,
            "score": 1,
            "solution": [
              {
                "name": "src/Task.kt",
                "text": "fun userFoo() {}",
                "is_visible": true
              },
              {
                "name": "test/MyTest.kt",
                "text": "fun myUpdatedTest() {}",
                "is_visible": true
              },
              {
                "name": "FileWithTeSt.py",
                "text": "pass",
                "is_visible": false
              }
            ],
            "language": null,
            "code": null,
            "version": 12,
            "feedback": {
              "message": "Congratulations!"
            },
            "check_profile": "some_check_profile"
          },
          "status": "correct",
          "step": 1
        }
      ]
    }
  """

  companion object {
    private const val TASK_FILE_NAME: String = "src/Task.kt"
    private const val FIRST_TASK_FILE_TEXT: String = "fun foo1() {}"
    private const val SECOND_TASK_FILE_TEXT: String = "fun foo2() {}"
    private const val FIRST_TEST_FILE_NAME: String = "FileWithTeSt.py"
    private const val SECOND_TEST_FILE_NAME: String = "FileWithTeSt.java"
    private const val USER_VISIBLE_TEST_FILE_NAME: String = "MyTest.kt"
  }
}