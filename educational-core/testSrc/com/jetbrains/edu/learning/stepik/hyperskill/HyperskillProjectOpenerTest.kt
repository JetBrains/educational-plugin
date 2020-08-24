package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.*

class HyperskillProjectOpenerTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector
  private val mockProjectManager: MockHyperskillProjectManager get() = HyperskillProjectManager.getInstance() as MockHyperskillProjectManager

  override fun setUp() {
    super.setUp()
    mockProjectManager.project = project
  }

  fun `test open stage in new project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    HyperskillProjectOpener.open(HyperskillOpenStageRequest(1, 1))

    val fileTree = fileTree {
      dir("Test Hyperskill Project") {
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
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open stage in opened project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    // set up existing project
    hyperskillCourseWithFiles {
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }

    HyperskillProjectOpener.open(HyperskillOpenStageRequest(1, 1))

    val fileTree = fileTree {
      dir("Problems") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
          }
          file("task.html")
        }
      }
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
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test open step in new code challenges project`() {
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

  fun `test open step in existing code challenges project`() {
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
    hyperskillCourseWithFiles(name = getCodeChallengesProjectName("TEXT"), language = PlainTextLanguage.INSTANCE) {
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

  fun `test open step in new project`() {
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

  fun `test open step in existing project with stages`() {
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

  private fun configureMockResponsesForStages() {
    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
          taskFile("test/Tests1.kt", "stage 1 test")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "stage 2")
          taskFile("test/Tests2.kt", "stage 2 test")
        }
      }
    })
  }

  override fun tearDown() {
    mockProjectManager.project = null
    super.tearDown()
  }
}