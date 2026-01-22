package com.jetbrains.edu.uiOnboarding.onboarding

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.ui.MessageType
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService
import com.jetbrains.edu.uiOnboarding.TransitionAnimator
import com.jetbrains.edu.uiOnboarding.steps.CodeEditorStep
import com.jetbrains.edu.uiOnboarding.steps.CourseViewStep
import com.jetbrains.edu.uiOnboarding.steps.ZhabaStepFactory
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.HAPPY_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.SAD_FINISH_TRANSITION
import io.mockk.*
import org.junit.Test

class TourTest : EduTestCase() {

  override fun setUp() {
    super.setUp()

    mockkObject(TransitionAnimator)
    every { TransitionAnimator.animateTransition(any(), any(), any(), any()) } returns null

    mockkObject(ZhabaStepFactory)

    val (lastStepId, lastTransition) = listOf(CodeEditorStep.STEP_KEY, SAD_FINISH_TRANSITION)
    every { ZhabaStepFactory.onboardingStep(any()) } answers {
      val stepId = firstArg<String>()
      val mockedStep = spyk(callOriginal())

      every { mockedStep.performStep(any(), any()) } returns mockk {
        every { zhaba } returns mockk()
      }

      coEvery { mockedStep.executeStep(any(), any(), any(), any()) } answers {
        if (stepId == lastStepId) {
          lastTransition
        }
        else {
          ZhabaStep.NEXT_TRANSITION
        }
      }

      mockedStep
    }
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
  fun `stopping the tour in the middle shows notification You can always call Toad from menu`() = doTestNotificationCalled(
    CodeEditorStep.STEP_KEY,
    SAD_FINISH_TRANSITION
  )

  @Test
  fun `stopping the tour at the end shows notification You can always call Toad from menu`() = doTestNotificationCalled(
    CourseViewStep.STEP_KEY,
    HAPPY_FINISH_TRANSITION
  )

  fun doTestNotificationCalled(lastStepId: String, lastTransition: String) {
    // Given

    // emulate that a user clicks "Next" until the step with the [lastStepId], and then chooses [lastTransition].


    // mock notifications
    mockkStatic(NotificationGroupManager::class)

    val mockNotification = mockk<Notification>(relaxed = true)
    val mockNotificationGroup = mockk<NotificationGroup>(relaxed = true)
    val mockNotificationGroupManager = mockk<NotificationGroupManager>(relaxed = true)

    every { NotificationGroupManager.getInstance() } returns mockNotificationGroupManager
    every { mockNotificationGroupManager.getNotificationGroup("EduOnboarding") } returns mockNotificationGroup
    every {
      mockNotificationGroup.createNotification(any<String>(), any<MessageType>())
    } returns mockNotification

    // create course
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }

    // When
    EduUiOnboardingService.getInstance(project).startOnboarding()

    // Then
    verify { mockNotification.notify(any()) }
  }
}