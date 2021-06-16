package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStageRequest
import com.jetbrains.edu.learning.withFeature


class HyperskillProjectOpenStageTest : HyperskillProjectOpenerTestBase() {

  fun `test open stage in new project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStageRequest(1, 1))

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

  fun `test open stage in opened legacy code problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()

    // set up existing project
    hyperskillCourseWithFiles {
      lesson(HYPERSKILL_PROBLEMS) {
        codeTask("task1", stepId = 4) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }

    withFeature(EduExperimentalFeatures.PROBLEMS_BY_TOPIC, false) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStageRequest(1, 1))
    }

    val fileTree = fileTree {
      dir(HYPERSKILL_PROBLEMS) {
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

  fun `test open stage in opened problems project`() {
    loginFakeUser()
    configureMockResponsesForStages()
    val topicName = "topicName"

    // set up existing project
    hyperskillCourseWithFiles {
      section(HYPERSKILL_TOPICS) {
        lesson(topicName) {
          codeTask("task1", stepId = 4) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
        }
      }
    }

    withFeature(EduExperimentalFeatures.PROBLEMS_BY_TOPIC, true) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStageRequest(1, 1))
    }

    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(topicName) {
          dir("task1") {
            dir("src") {
              file("Task.kt", "fun foo() {}")
            }
            file("task.html")
          }
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
}