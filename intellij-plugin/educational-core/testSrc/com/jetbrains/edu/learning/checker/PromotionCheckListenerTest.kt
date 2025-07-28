package com.jetbrains.edu.learning.checker

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.util.application
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockEduBrowser
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.*
import org.junit.Test

class PromotionCheckListenerTest : EduTestCase() {

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

    mockkObject(EduNotificationManager)

    val notificationAction = slot<AnAction>()
    val notification = mockk<Notification> {
      every { addAction(capture(notificationAction)) } returns this
      justRun { notify(any()) }
      justRun { expire() }
    }
    every { EduNotificationManager.create(any(), any(), any()) } returns notification

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    if (notificationAction.isCaptured) {
      val dataContext = SimpleDataContext.builder().add(Notification.KEY, notification).build()
      testAction(notificationAction.captured, dataContext)
    }

    // then
    verify { EduNotificationManager.create(any(), any(), any()) }
    verify { notification.notify(any()) }
    verify { notification.expire() }
    assertEquals(STUDENT_PACK_LINK, EduBrowser.getInstance().asSafely<MockEduBrowser>()?.lastVisitedUrl)
  }

  @Test
  fun `do not show promotion notification for the second time`() {
    // given
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

    mockkObject(EduNotificationManager)
    every { EduNotificationManager.create(any(), any(), any()) } returns mockk<Notification>()

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 0) { EduNotificationManager.create(any(), any(), any()) }
  }

  companion object {
    // Implementation detail from `RunOnceUtil`
    private const val PROPERTY_KEY = "RunOnceActivity.$STUDENT_PACK_PROMOTION_SHOWN_KEY"
  }
}
