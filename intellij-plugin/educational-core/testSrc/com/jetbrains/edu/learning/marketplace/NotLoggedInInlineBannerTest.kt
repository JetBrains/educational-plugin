package com.jetbrains.edu.learning.marketplace

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.marketplace.MarketplaceCheckListener.Companion.NOT_LOGGED_IN_BANNER_SHOWN_KEY
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.Test

class NotLoggedInInlineBannerTest : EduTestCase() {

  private lateinit var taskToolWindowView: TaskToolWindowView

  override fun setUp() {
    super.setUp()
    taskToolWindowView = mockService<TaskToolWindowView>(project)
    mockJBAccount(testRootDisposable)
  }

  @Test
  fun `test banner shown when not logged in`() {
    // given
    every { JBAccountInfoService.getInstance()?.userData } returns null
    bannerWasShownBefore(wasShown = false)
    val course = course()

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 1) { taskToolWindowView.addInlineBanner(any<NotLoggedInInlineBanner>()) }
  }

  @Test
  fun `test banner not shown for the second time`() {
    // given
    every { JBAccountInfoService.getInstance()?.userData } returns null
    bannerWasShownBefore(wasShown = true)
    val course = course()

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 0) { taskToolWindowView.addInlineBanner(any<NotLoggedInInlineBanner>()) }
  }

  @Test
  fun `test banner not shown when logged in`() {
    // given
    bannerWasShownBefore(wasShown = false)
    val course = course()

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 0) { taskToolWindowView.addInlineBanner(any<NotLoggedInInlineBanner>()) }
  }

  @Test
  fun `test banner not shown for non-marketplace course`() {
    // given
    every { JBAccountInfoService.getInstance()?.userData } returns null
    bannerWasShownBefore(wasShown = false)
    val course = course(isMarketplace = false)

    // when
    val task = course.findTask("lesson1", "task1")
    task.openTaskFileInEditor("foo.txt")
    testAction(CheckAction(task.getUICheckLabel()))

    // then
    verify(exactly = 0) { taskToolWindowView.addInlineBanner(any<NotLoggedInInlineBanner>()) }
  }

  private fun bannerWasShownBefore(wasShown: Boolean) {
    val properties = mockService<PropertiesComponent>(project)
    every { properties.updateValue(PROPERTY_KEY, any()) } returns !wasShown
    justRun { properties.setValue(PROPERTY_KEY, any<Boolean>()) }
  }

  private fun course(isMarketplace: Boolean = true): Course {
    val course = courseWithFiles(id = 1) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }.apply {
      this.isMarketplace = isMarketplace
    }
    return course
  }

  companion object {
    // Implementation detail from `RunOnceUtil`
    private const val PROPERTY_KEY = "RunOnceActivity.$NOT_LOGGED_IN_BANNER_SHOWN_KEY"
  }
}
