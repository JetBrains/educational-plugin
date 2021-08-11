package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_DATA_TASKS_SUPPORT
import com.jetbrains.edu.learning.EduNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.intellij.lang.annotations.Language
import java.text.SimpleDateFormat
import java.util.*

class HyperskillDownloadDatasetTest : EduActionTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  fun `test download new dataset`() {
    loginFakeUser()
    configureResponses()
    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(DATA_TASK_1, stepId = 1) {
            taskFile(TASK_HTML)
            taskFile(SOME_FILE_TXT)
          }
        }
      }
    }
    runAction(course, DATA_TASK_1)

    fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir(DATA_TASK_1) {
            file(TASK_HTML)
            file(SOME_FILE_TXT)
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                file(INPUT_FILE_NAME, TASK_1_DATASET_TEXT)
              }
            }
          }
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test update dataset`() {
    loginFakeUser()
    configureResponses()
    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(DATA_TASK_2,
                   attempt = Attempt(102, Date(), 300).toDataTaskAttempt(),
                   stepId = 2
          ) {
            taskFile(TASK_HTML)
            taskFile(SOME_FILE_TXT)
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                taskFile(INPUT_FILE_NAME, TASK_2_OLD_DATASET_TEXT)
              }
            }
          }
        }
      }
    }
    runAction(course, DATA_TASK_2)

    fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir(DATA_TASK_2) {
            file(TASK_HTML)
            file(SOME_FILE_TXT)
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                file(INPUT_FILE_NAME, TASK_2_NEW_DATASET_TEXT)
              }
            }
          }
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test outdated attempt`() {
    loginFakeUser()
    configureResponses()
    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(DATA_TASK_3,
                   attempt = Attempt(103, Date(), 0).toDataTaskAttempt(),
                   stepId = 3
          ) {
            taskFile(TASK_HTML)
            taskFile(SOME_FILE_TXT)
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                taskFile(INPUT_FILE_NAME, TASK_3_OLD_DATASET_TEXT)
              }
            }
          }
        }
      }
    }
    runAction(course, DATA_TASK_3)

    fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir(DATA_TASK_3) {
            file(TASK_HTML)
            file(SOME_FILE_TXT)
            dir(DATA_FOLDER_NAME) {
              dir(DATASET_FOLDER_NAME) {
                file(INPUT_FILE_NAME, TASK_3_NEW_DATASET_TEXT)
              }
            }
          }
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (request.path) {
          "/api/attempts" -> newAttemptForTask1
          "/api/attempts?step=1&user=1" -> noAttempts
          "/api/attempts?step=2&user=1" -> existingAttemptForTask2
          "/api/attempts?step=3&user=1" -> existingAttemptForTask3
          "/api/attempts/101/dataset" -> TASK_1_DATASET_TEXT
          "/api/attempts/102/dataset" -> TASK_2_NEW_DATASET_TEXT
          "/api/attempts/103/dataset" -> TASK_3_NEW_DATASET_TEXT
          else -> error("Wrong path: ${request.path}")
        }
      )
    }
  }

  private fun runAction(course: HyperskillCourse, taskName: String) {
    withFeature(HYPERSKILL_DATA_TASKS_SUPPORT, true) {
      val task = course.getLesson(HYPERSKILL_TOPICS, TOPIC_NAME)?.getTask(taskName) ?: error("Can't find `$taskName` file")
      NavigationUtils.navigateToTask(project, task, showDialogIfConflict = false)
      val taskFile = findFile("$HYPERSKILL_TOPICS/$TOPIC_NAME/$taskName/$SOME_FILE_TXT")
      testAction(DownloadDatasetAction.ACTION_ID, dataContext(taskFile))
    }
  }

  @Language("JSON")
  private val existingAttemptForTask2 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 102,
          "status": "active",
          "step": 2,
          "time": "${Date().format()}",
          "time_left": 300,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  private val existingAttemptForTask3 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 103,
          "status": "active",
          "step": 3,
          "time": "${Date().format()}",
          "time_left": 0,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  private val newAttemptForTask1 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 101,
          "status": "active",
          "step": 1,
          "time": "${Date(0).format()}",
          "time_left": null,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  private val noAttempts = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [ ]
    }
  """

  companion object {
    private const val DATA_TASK_1: String = "Data Task 1"
    private const val DATA_TASK_2: String = "Data Task 2"
    private const val DATA_TASK_3: String = "Data Task 3"
    private const val SOME_FILE_TXT: String = "someFile.txt"
    private const val TASK_1_DATASET_TEXT: String = "dataset text"
    private const val TASK_2_OLD_DATASET_TEXT: String = "old dataset for task 2 text"
    private const val TASK_2_NEW_DATASET_TEXT: String = "new dataset for task 2 text"
    private const val TASK_3_OLD_DATASET_TEXT: String = "old dataset for task 3 text"
    private const val TASK_3_NEW_DATASET_TEXT: String = "new dataset for task 3 text"
    private const val TOPIC_NAME: String = "Topic"

    private fun Date.format(): String {
      val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
      return formatter.format(this)
    }
  }
}