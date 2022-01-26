package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduNames.TASK_HTML
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATASET_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.testAction
import org.apache.http.HttpStatus
import java.util.*

class StepikDownloadDatasetTest : StepikBasedDownloadDatasetTest() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector

  override fun setUp() {
    super.setUp()
    EduSettings.getInstance().user = StepikUser.createEmptyUser().apply {
      userInfo = StepikUserInfo("Test User")
      userInfo.id = 1
    }
    configureResponses()
  }

  override fun tearDown() {
    EduSettings.getInstance().user = null
    super.tearDown()
  }

  fun `test download new dataset`() {
    val course = courseWithFiles(id = 1, language = PlainTextLanguage.INSTANCE) {
      section(SECTION) {
        lesson(LESSON) {
          dataTask(DATA_TASK_1, stepId = 1) {
            taskFile(TASK_HTML)
            taskFile(SOME_FILE_TXT)
          }
        }
      }
    }
    runAction(course, DATA_TASK_1)

    fileTree {
      dir(SECTION) {
        dir(LESSON) {
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
    val course = courseWithFiles(id = 1, language = PlainTextLanguage.INSTANCE) {
      section(SECTION) {
        lesson(LESSON) {
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
      dir(SECTION) {
        dir(LESSON) {
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
    val course = courseWithFiles(id = 1, language = PlainTextLanguage.INSTANCE) {
      section(SECTION) {
        lesson(LESSON) {
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
      dir(SECTION) {
        dir(LESSON) {
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
          "/api/attempts" -> {
            return@withResponseHandler MockResponseFactory.fromString(newAttemptForTask1, HttpStatus.SC_CREATED)
          }
          "/api/attempts?step=1&user=1" -> noAttempts
          "/api/attempts?step=2&user=1" -> existingAttemptForTask2
          "/api/attempts?step=3&user=1" -> existingAttemptForTask3
          "/api/attempts/101/file" -> TASK_1_DATASET_TEXT
          "/api/attempts/102/file" -> TASK_2_NEW_DATASET_TEXT
          "/api/attempts/103/file" -> TASK_3_NEW_DATASET_TEXT
          else -> error("Wrong path: ${request.path}")
        }
      )
    }
  }

  private fun runAction(course: Course, taskName: String) {
    val task = course.getLesson(SECTION, LESSON)?.getTask(taskName) ?: error("Can't find `$taskName` file")
    NavigationUtils.navigateToTask(project, task, showDialogIfConflict = false)
    val taskFile = findFile("$SECTION/$LESSON/$taskName/$SOME_FILE_TXT")
    testAction(DownloadDatasetAction.ACTION_ID, dataContext(taskFile))
  }

  companion object {
    private const val SECTION: String = "Section"
    private const val LESSON: String = "Lesson"
  }
}