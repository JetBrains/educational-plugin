package com.jetbrains.edu.learning.stepik.hyperskill.projectOpen

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockProjectOpener
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.logInFakeHyperskillUser
import com.jetbrains.edu.learning.stepik.hyperskill.logOutFakeHyperskillUser

abstract class HyperskillProjectOpenerTestBase : EduTestCase() {
  protected val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector
  protected val mockProjectOpener: MockProjectOpener get() = ProjectOpener.getInstance() as MockProjectOpener

  override fun setUp() {
    super.setUp()
    mockProjectOpener.project = project
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      mockProjectOpener.project = null
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

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

  companion object {
    const val THEORY = "Theory"

    abstract class ItemInfo(val id: Int) {
      abstract val file: String

      val path: String = "/api/steps"

      abstract val param: Pair<String, String>

      val urlWithPrams: String = "${path}?${param.first}=${param.second}"
    }

    open class StepInfo(id: Int, private val stepTitle: String? = null) : ItemInfo(id) {
      override val file: String
        get() = "step_${id}_response.json"

      override val param: Pair<String, String>
        get() = "ids" to id.toString()

      open val title: String
        get() = stepTitle ?: error("Title must be specified for step")
    }

    open class TopicInfo(id: Int, fileName: String = "steps_${id}_topic_response.json") : ItemInfo(id) {
      override val file: String = fileName

      override val param: Pair<String, String>
        get() = "topic" to id.toString()
    }
  }
}