package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectManager
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.MockHyperskillProjectManager
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourse

abstract class HyperskillProjectOpenerTestBase : EduTestCase() {
  protected val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector
  private val mockProjectManager: MockHyperskillProjectManager get() = HyperskillProjectManager.getInstance() as MockHyperskillProjectManager

  override fun setUp() {
    super.setUp()
    mockProjectManager.project = project
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  override fun tearDown() {
    mockProjectManager.project = null
    super.tearDown()
  }

  protected fun configureMockResponsesForStages() {
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
}