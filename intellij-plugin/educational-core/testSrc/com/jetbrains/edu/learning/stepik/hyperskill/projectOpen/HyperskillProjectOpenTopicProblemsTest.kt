package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult
import com.jetbrains.edu.learning.courseFormat.ext.PluginsRequired
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.hasParams
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import org.junit.Test


class HyperskillProjectOpenTopicProblemsTest : HyperskillProjectOpenerTestBase() {

  override fun setUp() {
    super.setUp()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()
  }

  @Test
  fun `test open code problem in project`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step2640.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2645.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2638.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open edu problem`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step10960.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step13292.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step13296.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step10960.title) {
            dir("src") {
              dir("calculator") {
                file("ArithmeticRestController.java")
                file("WebCalculatorApplication.java")
              }
            }
            dir("test") {
              file("WebCalculatorApplicationTest.java")
            }
            dir("resources") {
              file("application.properties")
            }
            file("task.html")
            file("build.gradle")
          }
          dir(step13293.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step13294.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step13295.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step7018.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step7020.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step13299.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open dataset problem without samples`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step14259.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step8139.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step8143.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step14259.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open dataset problem with samples`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step12164.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step10933.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step12164.title) {
            dir(DataTask.DATA_FOLDER_NAME) {
              dir(DataTask.DATA_SAMPLE_FOLDER_NAME) {
                file(DataTask.INPUT_FILE_NAME)
              }
            }
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open non dataset problem with language chosen by user`() {
    val request = HyperskillOpenStepWithProjectRequest(1, step10960.id, "TEXT", true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
  }

  @Test
  fun `test open problem with same topic in existing problems project`() {
    // set up existing project
    hyperskillCourseWithFiles(name = getProblemsProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(step9455.title) {
          theoryTask(THEORY, stepId = step9455.id) {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
          codeTask(step2640.title, stepId = step2640.id) {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
        }
      }
    }

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step2641.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2645.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2638.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem with different topic in existing problems project`() {
    // set up existing project
    hyperskillCourseWithFiles(name = getProblemsProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask("code task", stepId = 4) {
            taskFile("task.txt", "file text")
          }
        }
      }
    }

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, step2641.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir("code task") {
            file("task.txt", "file text")
            file("task.html")
          }
        }
        dir(step9455.title) {
          dir(THEORY) {
            file("Task.txt")
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2645.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2638.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem in existing project with stages`() {
    // set up existing project
    hyperskillCourseWithFiles {
      frameworkLesson(TEST_HYPERSKILL_PROJECT_NAME) {
        eduTask(testStageName(1), stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
          taskFile("test/Tests1.kt", "stage 1 test")
        }
        eduTask(testStageName(2), stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "stage 2 test")
        }
      }
    }

    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step2640.id, FakeGradleBasedLanguage.id)
    )
    val fileTree = fileTree {
      dir(TEST_HYPERSKILL_PROJECT_NAME) {
        dir("task") {
          dir("src") {
            file("Task.kt", "stage 1")
          }
          dir("test") {
            file("Tests1.kt", "stage 1 test")
          }
        }
        dir(testStageName(1)) {
          file("task.html")
        }
        dir(testStageName(2)) {
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem in existing project with stages and problems with same topic`() {
    // set up existing project
    hyperskillCourseWithFiles {
      frameworkLesson(TEST_HYPERSKILL_PROJECT_NAME) {
        eduTask(testStageName(1), stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
          taskFile("test/Tests1.kt", "stage 1 test")
        }
        eduTask(testStageName(2), stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "stage 2 test")
        }
      }
      section(HYPERSKILL_TOPICS) {
        lesson(step9455.title) {
          theoryTask(THEORY, stepId = step9455.id) {
            taskFile("src/Task.kt", "file text")
            taskFile("task.html", "file text")
          }
          codeTask(step2641.title, stepId = step2641.id) {
            taskFile("src/Task.kt", "file text")
            taskFile("task.html", "file text")
          }
        }
      }
    }

    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step2640.id, FakeGradleBasedLanguage.id)
    )
    val fileTree = fileTree {
      dir(TEST_HYPERSKILL_PROJECT_NAME) {
        dir("task") {
          dir("src") {
            file("Task.kt", "stage 1")
          }
          dir("test") {
            file("Tests1.kt", "stage 1 test")
          }
        }
        dir(testStageName(1)) {
          file("task.html")
        }
        dir(testStageName(2)) {
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem in existing project with stages and problems with different topic`() {
    // set up existing project
    hyperskillCourseWithFiles {
      frameworkLesson(TEST_HYPERSKILL_PROJECT_NAME) {
        eduTask(testStageName(1), stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
          taskFile("test/Tests1.kt", "stage 1 test")
        }
        eduTask(testStageName(2), stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "stage 2 test")
        }
      }
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask("code task 4", stepId = 4) {
            taskFile("task.txt", "file text")
          }
          codeTask("code task 5", stepId = 5) {
            taskFile("task.txt", "file text")
          }
        }
      }
    }

    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step2641.id, FakeGradleBasedLanguage.id)
    )
    val fileTree = fileTree {
      dir(TEST_HYPERSKILL_PROJECT_NAME) {
        dir("task") {
          dir("src") {
            file("Task.kt", "stage 1")
          }
          dir("test") {
            file("Tests1.kt", "stage 1 test")
          }
        }
        dir(testStageName(1)) {
          file("task.html")
        }
        dir(testStageName(2)) {
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir("code task 4") {
            file("task.txt", "file text")
            file("task.html")
          }
          dir("code task 5") {
            file("task.txt", "file text")
            file("task.html")
          }
        }
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem in existing project with legacy code problems`() {
    // set up existing project
    hyperskillCourseWithFiles {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask("code task", stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    }

    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step2640.id, FakeGradleBasedLanguage.id)
    )
    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
        dir("code task") {
          file("task.txt", "file text")
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open problem in existing project with stages and legacy code problems`() {
    // set up existing project
    hyperskillCourseWithFiles {
      frameworkLesson(TEST_HYPERSKILL_PROJECT_NAME) {
        eduTask(testStageName(1), stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
          taskFile("test/Tests1.kt", "stage 1 test")
        }
        eduTask(testStageName(2), stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "stage 2 test")
        }
      }
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask("code task", stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    }

    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step2640.id, FakeGradleBasedLanguage.id)
    )
    val fileTree = fileTree {
      dir(TEST_HYPERSKILL_PROJECT_NAME) {
        dir("task") {
          dir("src") {
            file("Task.kt", "stage 1")
          }
          dir("test") {
            file("Tests1.kt", "stage 1 test")
          }
        }
        dir(testStageName(1)) {
          file("task.html")
        }
        dir(testStageName(2)) {
          file("task.html")
        }
      }
      dir(HYPERSKILL_PROBLEMS) {
        dir("code task") {
          file("task.txt", "file text")
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }


  @Test
  fun `test open code problem without selected project in new project`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepRequest(step2640.id, FakeGradleBasedLanguage.id))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test open non dataset problem with language chosen by user without selected project`() {
    val request = HyperskillOpenStepRequest(step10960.id, FakeGradleBasedLanguage.id, true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
  }

  @Test
  fun `test open code problem without selected project in existing project`() {
    // set up existing project
    hyperskillCourseWithFiles {
      frameworkLesson(TEST_HYPERSKILL_PROJECT_NAME) {}
    }

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepRequest(step2640.id, FakeGradleBasedLanguage.id))
    val fileTree = fileTree {
      dir(TEST_HYPERSKILL_PROJECT_NAME) {}
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(THEORY) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step525047.title) {
            file(AnswerTask.ANSWER_FILE_NAME)
            file("task.html")
          }
          dir(step2640.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2645.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2641.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
          dir(step2638.title) {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test unknown language`() {
    val unknownLanguage = "Unknown language"
    doLanguageValidationTest(unknownLanguage) {
      assertEquals(EduCoreBundle.message("hyperskill.unsupported.language", unknownLanguage), it.message)
    }
  }

  @Test
  fun `test language supported with plugin`() {
    doLanguageValidationTest("python") {
      assertTrue("actual: $it", it is PluginsRequired)
    }
  }

  @Test
  fun `test language not supported in IDE`() {
    val unsupportedLanguage = "Unsupported"
    doLanguageValidationTest(unsupportedLanguage) {
      val expectedMessage = EduCoreBundle.message(
        "rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
        unsupportedLanguage
      )
      assertEquals(expectedMessage, it.message)
    }
  }

  private fun doLanguageValidationTest(language: String, checkError: (CourseValidationResult) -> Unit) {
    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask(stepId = 4) {
            taskFile("task.txt", "file text")
          }
        }
      }
    })

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, 4, language)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }

  private fun configureMockResponsesForProblems() {
    requestedInformation.forEach { information ->
      mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
        if (request.pathWithoutPrams.endsWith(information.path) && request.hasParams(information.param)) {
          mockResponse(information.file)
        }
        else null
      }
    }
  }

  companion object {
    private const val TOPIC_NAME = "topicName"

    private val step9455 = StepInfo(9455, "Wildcards")
    private val step2638 = StepInfo(2638, "The result of the code")
    private val step2640 = StepInfo(2640, "Packing bakeries")
    private val step2641 = StepInfo(2641, "List multiplicator")
    private val step2645 = StepInfo(2645, "Find an equivalent")
    private val step13296 = StepInfo(13296, "Post mapping")
    private val step13293 = StepInfo(13293, "Passing parameters")
    private val step13294 = StepInfo(13294, "Complete the method")
    private val step13295 = StepInfo(13295, "DeleteMapping request")
    private val step7018 = StepInfo(7018, "@GetMapping and @PostMapping")
    private val step13299 = StepInfo(13299, "Complete the handler description")
    private val step7020 = StepInfo(7020, "Find a handler for the request")

    private val step525047 = StepInfo(525047, "Fix signature")

    @Suppress("unused") // not recommended step
    private val step9886 = StepInfo(9886, "Pets in boxes")
    private val topic85 = TopicInfo(85)

    private val step13292 = StepInfo(13292, "Posting and deleting data via REST")
    private val step10960 = StepInfo(10960, "Web calculator")
    private val topic515 = TopicInfo(515)

    private val step8139 = StepInfo(8139, "Reading files")

    @Suppress("unused") // not recommended step
    private val step8146 = StepInfo(8146, "Acronym")
    private val step8143 = StepInfo(8143, "First")
    private val step14259 = StepInfo(14259, "Summer")
    private val topic632 = TopicInfo(632)

    private val step10933 = StepInfo(10933, "DataFrame")
    private val step12164 = StepInfo(12164, "The shape of a data frame")
    private val topic1034 = TopicInfo(1034)

    private val requestedInformation = listOf(
      step2640, step2641, topic85,
      step10960, topic515,
      step14259, topic632,
      step12164, topic1034
    )
  }
}