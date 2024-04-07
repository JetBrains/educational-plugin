package com.jetbrains.edu.shell.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.hasParams
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import com.jetbrains.edu.shell.ShellConfigurator.Companion.TASK_SH

class ShellHyperskillProjectOpenTopicProblemsTest : HyperskillProjectOpenerTestBase() {
  override fun getTestDataPath(): String = "testData/hyperskill/"

  override fun setUp() {
    super.setUp()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()
  }

  fun `test open code problem in project`() {
    mockProjectOpener.open(
      HyperskillOpenInIdeRequestHandler,
      HyperskillOpenStepWithProjectRequest(1, step12720.id, HyperskillLanguages.SHELL.requestLanguage)
    )
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(step12713.title) {
          dir(THEORY) {
            file(TASK_SH)
            file("task.html")
          }
          dir(step12720.title) {
            file(TASK_SH)
            file("task.html")
          }
        }
      }
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  private fun configureMockResponsesForProblems() {
    requestedInformation.forEach { information ->
      mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
        if (request.pathWithoutPrams.endsWith(information.path) && request.hasParams(information.param)) {
          mockResponse(information.file)
        }
        else {
          null
        }
      }
    }
  }

  companion object {
    private val step12713 = StepInfo(12713, "Grep basic options")
    private val step12720 = StepInfo(12720, "Me")
    private val topic85 = TopicInfo(1316)

    private val requestedInformation = listOf(step12713, step12720, topic85)
  }
}