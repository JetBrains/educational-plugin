package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStageRequest
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectManager
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.MockHyperskillProjectManager

class HyperskillProjectOpenerTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector
  private val mockProjectManager: MockHyperskillProjectManager get() = HyperskillProjectManager.getInstance() as MockHyperskillProjectManager

  override fun setUp() {
    super.setUp()
    mockProjectManager.project = project
  }

  fun `test open stage in new project`() {
    loginFakeUser()
    configureMockResponses()

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
    configureMockResponses()

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

  private fun configureMockResponses() {
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