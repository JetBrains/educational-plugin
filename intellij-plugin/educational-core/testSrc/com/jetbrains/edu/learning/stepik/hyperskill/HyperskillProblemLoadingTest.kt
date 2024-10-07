package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.hasParams
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler.getStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import org.junit.Test

class HyperskillProblemLoadingTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test load step with hidden header and footer`() = doTest("steps_response_header_footer.json", shouldDisableHighlight = true)
  @Test
  fun `test load step with hidden header`() = doTest("steps_response_header.json", shouldDisableHighlight = true)
  @Test
  fun `test load step with hidden footer`() = doTest("steps_response_footer.json", shouldDisableHighlight = true)
  @Test
  fun `test load step without hidden header or footer`() = doTest("steps_response_no_header_footer.json", shouldDisableHighlight = false)

  private fun doTest(responseFileName: String, shouldDisableHighlight: Boolean) {
    configureResponse(responseFileName)
    val course = createHyperskillCourse()
    val task = course.getTopicsSection()?.getLesson(step4894.title)?.getTask(step4894.title)
               ?: error("Can't find task from topics section")
    for ((_, taskFile) in task.taskFiles) {
      assertEquals(shouldDisableHighlight, taskFile.errorHighlightLevel == EduFileErrorHighlightLevel.NONE)
    }
  }

  private fun createHyperskillCourse(): HyperskillCourse {
    val course = courseWithFiles(
      language = PlainTextLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))
    val request = HyperskillOpenStepWithProjectRequest(1, 4894, "TEXT")
    course.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
    return course
  }

  private fun configureResponse(responseFileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      if (request.pathWithoutPrams.endsWith(step4894.path) && request.hasParams(step4894.param)) {
        mockResponse(responseFileName)
      }
      else if (request.pathWithoutPrams.endsWith(topic84.path) && request.hasParams(topic84.param)) {
        mockResponse(responseFileName)
      }
      else null
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  companion object {
    private val step4894 = StepInfo(4894, "Violator")
    private val topic84 = TopicInfo(84)
  }
}