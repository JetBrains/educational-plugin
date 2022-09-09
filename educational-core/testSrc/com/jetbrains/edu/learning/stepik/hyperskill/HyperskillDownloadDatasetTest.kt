package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.StepikBasedDownloadDatasetTest
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.ide.BuiltInServerManager
import java.util.*

class HyperskillDownloadDatasetTest : StepikBasedDownloadDatasetTest() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
    configureResponses()
  }

  override fun tearDown() {
    logOutFakeHyperskillUser()
    super.tearDown()
  }

  fun `test download new dataset`() {
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
    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(
            DATA_TASK_2,
            stepId = 2,
            attempt = Attempt(102, Date(), 300).toDataTaskAttempt()
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
    val course = hyperskillCourseWithFiles(language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          dataTask(
            DATA_TASK_3,
            stepId = 3,
            attempt = Attempt(103, Date(), 0).toDataTaskAttempt()
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

  companion object {
    private const val TOPIC_NAME: String = "Topic"
  }
}