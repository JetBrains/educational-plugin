package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener
import com.jetbrains.edu.learning.withFeature


class HyperskillProjectOpenTopicProblemsTest : HyperskillProjectOpenerTestBase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(EduExperimentalFeatures.PROBLEMS_BY_TOPIC, true) {
      super.runTestRunnable(context)
    }
  }

  fun `test open problem in project`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2640.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem with same topic in existing problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

    // set up existing project
    hyperskillCourseWithFiles(name = getProblemsProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
      section(HYPERSKILL_TOPICS) {
        lesson(step9455.title) {
          codeTask(step2640.title, stepId = step2640.id) {
            taskFile("Task.txt", "file text")
            taskFile("task.html", "file text")
          }
        }
      }
    }

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2641.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem with different topic in existing problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

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

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2641.id, "TEXT"))
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(TOPIC_NAME) {
          dir("code task") {
            file("task.txt", "file text")
            file("task.html")
          }
        }
        dir(step9455.title) {
          dir(step2640.title) {
            file("Task.txt")
            file("task.html")
          }
          dir(step2641.title) {
            file("Task.txt")
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with stages`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

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

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2640.id, FakeGradleBasedLanguage.id))
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
          dir(step2640.title) {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with stages and problems with same topic`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

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
          codeTask(step2641.title, stepId = step2641.id) {
            taskFile("src/Task.kt", "file text")
            taskFile("task.html", "file text")
          }
        }
      }
    }

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2640.id, FakeGradleBasedLanguage.id))
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
          dir(step2640.title) {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with stages and problems with different topic`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

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

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2641.id, FakeGradleBasedLanguage.id))
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
          dir(step2640.title) {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with legacy code problems`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

    // set up existing project
    hyperskillCourseWithFiles {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask("code task", stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    }

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2640.id, FakeGradleBasedLanguage.id))
    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
        dir("code task") {
          file("task.txt", "file text")
          file("task.html")
        }
      }
      dir(HYPERSKILL_TOPICS) {
        dir(step9455.title) {
          dir(step2640.title) {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with stages and legacy code problems`() {
    loginFakeUser()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()

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

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, step2640.id, FakeGradleBasedLanguage.id))
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
          dir(step2640.title) {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test unknown language`() {
    val unknownLanguage = "Unknown language"
    doLanguageValidationTest(unknownLanguage) {
      assertEquals(EduCoreBundle.message("hyperskill.unsupported.language", unknownLanguage), it)
    }
  }

  fun `test language supported with plugin`() {
    doLanguageValidationTest("python") {
      assertTrue("actual: $it", it.contains(EduCoreBundle.message("course.dialog.error.plugin.install.and.enable")))
    }
  }

  fun `test language not supported in IDE`() {
    val unsupportedLanguage = "Unsupported"
    doLanguageValidationTest(unsupportedLanguage) {
      val expectedMessage = EduCoreBundle.message("hyperskill.language.not.supported", ApplicationNamesInfo.getInstance().productName,
                                                  unsupportedLanguage)
      assertEquals(expectedMessage, it)
    }
  }

  private fun doLanguageValidationTest(language: String, checkError: (String) -> Unit) {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask(stepId = 4) {
            taskFile("task.txt", "file text")
          }
        }
      }
    })

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 4, language)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }

  private fun configureMockResponsesForProblems() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      when {
        request.path.endsWith(STEPS_OF_85_TOPIC_RECOMMENDED_REQUEST_SUFFIX) -> {
          mockResponse("steps_85_topic_recommended_response.json")
        }
        request.path.endsWith(STEPS_OF_85_TOPIC_REQUEST_SUFFIX) -> {
          mockResponse("steps_85_topic_response.json")
        }
        else -> null
      }
    }

    steps.forEach { step ->
      mockConnector.withResponseHandler(testRootDisposable) { request ->
        if (request.path.endsWith(step.path)) {
          mockResponse("step_${step.id}_response.json")
        }
        else null
      }
    }
  }

  companion object {
    private const val STEPS_OF_85_TOPIC_RECOMMENDED_REQUEST_SUFFIX = "/api/steps?topic=85&is_recommended=true"
    private const val STEPS_OF_85_TOPIC_REQUEST_SUFFIX = "/api/steps?topic=85"
    private const val TOPIC_NAME = "topicName"
    private val step2640 = StepInfo(2640, "Packing bakeries")
    private val step2641 = StepInfo(2641, "List multiplicator")
    private val step9455 = StepInfo(9455, "Wildcards")
    private val step9886 = StepInfo(9886, "Pets in boxes")
    private val steps = listOf(step2640, step2641, step9455, step9886)

    private class StepInfo(val id: Int, val title: String) {
      val path: String
        get() = "/api/steps?ids=$id"
    }
  }
}