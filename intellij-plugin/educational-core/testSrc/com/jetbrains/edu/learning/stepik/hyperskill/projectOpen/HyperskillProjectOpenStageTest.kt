package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.TEST_HYPERSKILL_PROJECT_NAME
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import com.jetbrains.edu.learning.stepik.hyperskill.testStageName


class HyperskillProjectOpenStageTest : HyperskillProjectOpenerTestBase() {

  override fun setUp() {
    super.setUp()
    configureMockResponsesForStages()
  }

  fun `test open stage in new project`() {
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenProjectStageRequest(1, 1))

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

  fun `test open stage in opened problems project`() {
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

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenProjectStageRequest(1, 1))

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