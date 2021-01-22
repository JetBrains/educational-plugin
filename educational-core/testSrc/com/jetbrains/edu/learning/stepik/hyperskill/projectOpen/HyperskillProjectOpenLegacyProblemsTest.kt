package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener


/** TODO Remove this test after [EduExperimentalFeatures.PROBLEMS_BY_TOPIC] feature is become enabled by default */
class HyperskillProjectOpenLegacyProblemsTest : HyperskillProjectOpenerTestBase() {
  override fun runTestInternal(context: TestContext) {
    withFeature(EduExperimentalFeatures.PROBLEMS_BY_TOPIC, false) {
      super.runTestInternal(context)
    }
  }

  fun `test open problem in new legacy code problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null, language = PlainTextLanguage.INSTANCE) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask(stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    })

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 4, "TEXT"))

    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
        dir("task1") {
          file("task.txt", "file text")
          file("task.html")
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing legacy code problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null, language = PlainTextLanguage.INSTANCE) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask(name = "new task", stepId = 5) {
          taskFile("task.txt", "new file text")
        }
      }
    })

    // set up existing project
    hyperskillCourseWithFiles(name = getLegacyProblemsProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask("code task", stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    }

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 5, "TEXT"))

    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
        dir("code task") {
          file("task.txt", "file text")
          file("task.html")
        }
        dir("new task") {
          file("task.txt", "new file text")
          file("task.html")
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in new project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask(stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    })

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 4, FakeGradleBasedLanguage.id))
    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
        dir("task1") {
          file("task.txt", "file text")
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open problem in existing project with stages`() {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask(stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    })

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

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 4, FakeGradleBasedLanguage.id))

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
        dir("task1") {
          file("task.txt", "file text")
          file("task.html")
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
      val expectedMessage = EduCoreBundle.message(
        "hyperskill.language.not.supported", ApplicationNamesInfo.getInstance().productName,
        unsupportedLanguage
      )
      assertEquals(expectedMessage, it)
    }
  }

  private fun doLanguageValidationTest(language: String, checkError: (String) -> Unit) {
    loginFakeUser()
    configureMockResponsesForStages()

    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask(stepId = 4) {
          taskFile("task.txt", "file text")
        }
      }
    })

    HyperskillProjectOpener.open(HyperskillOpenStepRequest(1, 4, language)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }
}