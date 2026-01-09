package com.jetbrains.edu.uiOnboarding.checker

import com.intellij.ide.util.PropertiesComponent
import com.intellij.util.application
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.edu.uiOnboarding.GotItBalloonGraphData
import com.jetbrains.edu.uiOnboarding.TransitionAnimator
import com.jetbrains.edu.uiOnboarding.steps.ZhabaStepFactory
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import io.mockk.*
import org.junit.Test

class PromotionCheckListenerTest : EduTestCase() {

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
  fun `show promotion notification for the first time`() {
    // given

    // mocks for ZhabaGraph from the setUp() method

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }

    val properties = mockService<PropertiesComponent>(application)
    every { properties.updateValue(PROPERTY_KEY, any()) } returns true
    justRun { properties.setValue(PROPERTY_KEY, any<Boolean>()) }

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    assertEquals(STUDENT_PACK_LINK, EduBrowser.getInstance().asSafely<MockEduBrowser>()?.lastVisitedUrl)
  }

  @Test
  fun `do not show promotion notification for the second time`() {
    // given

    // mocks for ZhabaGraph from the setUp() method

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }

    val properties = mockService<PropertiesComponent>(application)
    every { properties.updateValue(PROPERTY_KEY, any()) } returns false
    justRun { properties.setValue(PROPERTY_KEY, any<Boolean>()) }

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    assertEquals(null, EduBrowser.getInstance().asSafely<MockEduBrowser>()!!.lastVisitedUrl)
  }

  @Test
  fun `do not show promotion notification for the preview course`() {
    // given

    // mocks for ZhabaGraph from the setUp() method

    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    } as EduCourse
    course.isPreview = true

    val properties = mockService<PropertiesComponent>(application)
    every { properties.updateValue(PROPERTY_KEY, any()) } returns true
    justRun { properties.setValue(PROPERTY_KEY, any<Boolean>()) }

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    assertEquals(null, EduBrowser.getInstance().asSafely<MockEduBrowser>()?.lastVisitedUrl)
  }

  companion object {
    // Implementation detail from `RunOnceUtil`
    private const val PROPERTY_KEY = "RunOnceActivity.$STUDENT_PACK_PROMOTION_SHOWN_KEY"
  }
}