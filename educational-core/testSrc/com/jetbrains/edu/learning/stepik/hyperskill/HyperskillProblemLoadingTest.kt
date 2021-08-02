package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler.addProblem
import com.jetbrains.edu.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import com.jetbrains.edu.learning.withFeature

class HyperskillProblemLoadingTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(EduExperimentalFeatures.PROBLEMS_BY_TOPIC, true) {
      super.runTestRunnable(context)
    }
  }

  override fun setUp() {
    super.setUp()
    loginFakeUser()
  }

  fun `test load step with hidden header and footer`() = doTest("steps_response_header_footer.json", shouldContainWarning = true)
  fun `test load step with hidden header`() = doTest("steps_response_header.json", shouldContainWarning = true)
  fun `test load step with hidden footer`() = doTest("steps_response_footer.json", shouldContainWarning = true)
  fun `test load step without hidden header or footer`() = doTest("steps_response_no_header_footer.json", shouldContainWarning = false)

  private fun doTest(responseFileName: String, shouldContainWarning: Boolean) {
    configureResponse(responseFileName)
    val course = createHyperskillCourse()
    val task = course.getTopicsSection()?.getLesson(step4894.title)?.getTask(step4894.title)
               ?: error("Can't find task from topics section")
    assertEquals(shouldContainWarning,
                 EduCoreBundle.message("hyperskill.hidden.content", EduCoreBundle.message("check.title")) in task.descriptionText)
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
    course.addProblem(4894)
    return course
  }

  private fun configureResponse(responseFileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      if (request.path.endsWith(step4894.path)) {
        mockResponse(responseFileName)
      }
      else if (request.path.endsWith("/api/steps?topic=84")) {
        mockResponse(responseFileName)
      }
      else null
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  companion object {
    private val step4894 = StepInfo(4894, "Violator")
  }
}