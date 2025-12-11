package com.jetbrains.edu.uiOnboarding

import com.jetbrains.edu.uiOnboarding.steps.ZhabaStepFactory
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.testgraph.ZhabaExecutionTestCase
import com.jetbrains.edu.uiOnboarding.testgraph.zhabaGraph
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.junit.Test

class ZhabaExecutorTest : ZhabaExecutionTestCase() {

  override fun setUp() {
    super.setUp()

    mockkObject(TransitionAnimator)
    every { TransitionAnimator.animateTransition(any(), any(), any(), any()) } returns null

    mockkObject(ZhabaStepFactory)

    val studentPackPromotionStep = ZhabaStepFactory.studentPackPromotionStep()
    val mockedStudentPackPromotionStep = spyk(studentPackPromotionStep)

    // when the step is performed, it should return a non-empty result so that the executor does not skip it
    every { mockedStudentPackPromotionStep.performStep(any(), any()) } returns mockk {
      every { zhaba } returns mockk()
    }

    // We are not going to wait when a user clicks the "Apply Now" button, so we pretend that when the step is executed,
    // the button is pressed immediately:
    coEvery { mockedStudentPackPromotionStep.executeStep(any(), any(), any(), any()) } answers {
      mockedStudentPackPromotionStep.onPrimaryButton(GotItBalloonGraphData(null, 1))
      return@answers FINISH_TRANSITION
    }

    every { ZhabaStepFactory.studentPackPromotionStep() } returns mockedStudentPackPromotionStep
  }

  override fun tearDown() {
    try {
      clearAllMocks()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `scenario testing`() {
    val graph = zhabaGraph {
      "step1" via "next" to "step2"
    }

    zhabaScenario(graph, "step1") {
      expectStep("step1")
      expectTransition("next")
      expectStep("step2")
    }
  }
}