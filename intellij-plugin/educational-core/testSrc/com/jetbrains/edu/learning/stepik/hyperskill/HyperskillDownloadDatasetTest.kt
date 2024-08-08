package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.intellij.lang.annotations.Language
import org.jetbrains.ide.BuiltInServerManager
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class HyperskillDownloadDatasetTest : EduActionTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
    configureResponses()
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
  fun `test download new dataset`() {
    val taskHtml = DescriptionFormat.HTML.fileName

    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(DATA_TASK_1, stepId = 1) {
            taskFile(taskHtml)
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
            file(taskHtml)
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

  @Test
  fun `test update dataset`() {
    val taskHtml = DescriptionFormat.HTML.fileName

    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(
            DATA_TASK_2,
            stepId = 2,
            attempt = Attempt(102, Date(), 300).toDataTaskAttempt()
          ) {
            taskFile(taskHtml)
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
            file(taskHtml)
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

  @Test
  fun `test outdated attempt`() {
    val taskHtml = DescriptionFormat.HTML.fileName

    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(
            DATA_TASK_3,
            stepId = 3,
            attempt = Attempt(103, Date(), 0).toDataTaskAttempt()
          ) {
            taskFile(taskHtml)
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
            file(taskHtml)
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

  private fun configureResponses() {
    val port = BuiltInServerManager.getInstance().port.toString()
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      val data = when (val path = request.pathWithoutPrams) {
        "/api/attempts" -> when {
          request.hasParams("step" to "1", "user" to "1", "ide_rpc_port" to port) -> noAttempts
          request.hasParams("step" to "2", "user" to "1", "ide_rpc_port" to port) -> existingAttemptForTask2
          request.hasParams("step" to "3", "user" to "1", "ide_rpc_port" to port) -> existingAttemptForTask3
          request.hasParams("ide_rpc_port" to port) -> newAttemptForTask1
          else -> error("Wrong path: $path")
        }
        "/api/attempts/101/dataset" -> TASK_1_DATASET_TEXT
        "/api/attempts/102/dataset" -> TASK_2_NEW_DATASET_TEXT
        "/api/attempts/103/dataset" -> TASK_3_NEW_DATASET_TEXT
        else -> error("Wrong path: $path")
      }
      MockResponseFactory.fromString(data)
    }
  }

  private fun runAction(course: HyperskillCourse, taskName: String) {
    val task = course.getLesson(HYPERSKILL_TOPICS, TOPIC_NAME)?.getTask(taskName) ?: error("Can't find `$taskName` file")
    NavigationUtils.navigateToTask(project, task, showDialogIfConflict = false)
    val taskFile = findFile("$HYPERSKILL_TOPICS/$TOPIC_NAME/$taskName/$SOME_FILE_TXT")
    testAction(DownloadDatasetAction.ACTION_ID, dataContext(taskFile))
  }

  private fun now(): String? {
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    return ZonedDateTime.now(ZoneId.of("GMT0")).format(format)
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
          "time": "${now()}",
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
          "time": "${now()}",
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
          "time": "${now()}",
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
    private const val TOPIC_NAME: String = "Topic"

    private const val DATA_TASK_1: String = "Data Task 1"
    private const val DATA_TASK_2: String = "Data Task 2"
    private const val DATA_TASK_3: String = "Data Task 3"
    private const val SOME_FILE_TXT: String = "someFile.txt"
    private const val TASK_1_DATASET_TEXT: String = "dataset text"
    private const val TASK_2_OLD_DATASET_TEXT: String = "old dataset for task 2 text"
    private const val TASK_2_NEW_DATASET_TEXT: String = "new dataset for task 2 text"
    private const val TASK_3_OLD_DATASET_TEXT: String = "old dataset for task 3 text"
    private const val TASK_3_NEW_DATASET_TEXT: String = "new dataset for task 3 text"
  }
}