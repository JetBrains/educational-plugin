package com.jetbrains.edu.coursecreator.stepik.hyperskill

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.ApplyHyperskillSubmission
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.intellij.lang.annotations.Language

class ApplyHyperskillSubmissionTest: EduActionTestCase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  fun `test apply web submission`() = doTest(submissionFromWeb)

  fun `test submission from ide`() = doTest(submissionFromIDE)

  fun `test in educator mode`() = doTest(submissionFromWeb, CourseMode.COURSE_MODE, fileTree {
    dir("test project") {
      dir("task1") {
        file("task.html")
        dir("src") {
          file("Main.kt", "file text")
        }
        dir("test") {
          file("Test.java", "tests 1")
        }
      }
      dir("task2") {
        file("task.html")
        dir("src") {
          file("Task.kt", "stage 2")
        }
        dir("test") {
          file("Tests2.kt", "tests 2")
        }
      }
      dir("task3") {
        file("task.html")
        dir("src") {
          file("Task.kt", "stage 3")
        }
        dir("test") {
          file("Tests3.kt", "tests 3")
        }
      }
    }
    file("build.gradle")
    file("settings.gradle")
  })

  private fun doTest(submissionsJson: String, mode: CourseMode = CourseMode.STUDY, expectedFileTree: FileTree = learnerExpectedFileTree) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (request.path) {
          "/api/submissions/1" -> submissionsJson
          else -> "{}"
        }
      )
    }

    createHyperskillCourse(mode)
    FileEditorManager.getInstance(project).openFile(findFileInTask(0, 0, "src/Main.kt"), true)

    withEduTestDialog(EduTestInputDialog("1")) {
      val taskDir = findTask(0, 0).getDir(project.courseDir) ?: error("Task directory not found")
      testAction(ApplyHyperskillSubmission.ACTION_ID, dataContext(taskDir))
    }

    expectedFileTree.assertEquals(project.courseDir, myFixture)
  }

  private fun createHyperskillCourse(mode: CourseMode): HyperskillCourse {
    val course = courseWithFiles(
      courseMode = mode,
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("test project") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Main.kt", "stage 1")
          taskFile("test/Test.java", "tests 1")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "tests 2")
        }
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt", "stage 3")
          taskFile("test/Tests3.kt", "tests 3")
        }
      }
    } as HyperskillCourse

    if (course.isStudy) {
      course.hyperskillProject = HyperskillProject()
      course.stages = listOf(HyperskillStage(1, "", 1), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))
    }
    return course
  }

  @Language("JSON")
  private val submissionFromWeb = """
      {
        "meta": {
          "page": 1,
          "has_next": false,
          "has_previous": false
        },
        "submissions": [
          {
            "id": 1,
            "status": "correct",
            "score": 1,
            "hint": "",
            "feedback": "",
            "time": "2020-06-13T03:39:28Z",
            "reply": {
              "score": "",
              "solution": [
                {
                  "name": "test/Test.java",
                  "text": "test text",
                  "is_visible": false
                },
                {
                  "name": "src/Main.kt",
                  "text": "file text",
                  "is_visible": true
                }
              ],
              "check_profile": "hyperskill_gradle"
            },
            "reply_url": null,
            "attempt": 217713700,
            "session": null,
            "eta": 0
          }
        ]
      }
    """.trimIndent()

  @Language("JSON")
  private val submissionFromIDE = """
      {
        "meta": {
          "page": 1,
          "has_next": false,
          "has_previous": false
        },
        "submissions": [
          {
            "id": 1,
            "status": "wrong",
            "score": 0,
            "hint": "hint",
            "feedback": {
              "message": "feedback message"
            },
            "time": "2020-06-13T09:14:34Z",
            "reply": {
              "choices": null,
              "score": "0",
              "solution": [
                {
                  "name": "test/Test.java",
                  "text": "test text",
                  "is_visible": false
                },
                {
                  "name": "src/Main.kt",
                  "text": "file text",
                  "is_visible": true
                }
              ],
              "language": null,
              "code": null,
              "edu_task": null,
              "version": 11,
              "feedback": {
                "message": "feedback message"
              },
              "check_profile": ""
            },
            "reply_url": null,
            "attempt": 217717325,
            "session": null,
            "eta": 0
          }
        ]
      }
    """.trimIndent()

  private val learnerExpectedFileTree = fileTree {
    dir("test project") {
      dir("task") {
        dir("src") {
          file("Main.kt", "file text")
        }
        dir("test") {
          file("Test.java", "tests 1")
        }
      }
      dir("task1") {
        file("task.html")
      }
      dir("task2") {
        file("task.html")
      }
      dir("task3") {
        file("task.html")
      }
    }
    file("build.gradle")
    file("settings.gradle")
  }
}
