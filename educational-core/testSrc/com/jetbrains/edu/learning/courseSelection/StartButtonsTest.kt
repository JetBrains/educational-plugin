package com.jetbrains.edu.learning.courseSelection

import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

class StartButtonsTest : EduTestCase() {

  fun `test edu course`() {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val panel = MarketplacePlatformProvider().createPanel(scope, testRootDisposable)
    val course = course { }
    val coursePanel = panel.coursePanel
    coursePanel.bindCourse(course)

    val startButton = coursePanel.buttonsPanel.buttons.first()
    assertTrue(startButton.isVisible)
    assertTrue(startButton.isEnabled)
  }

  fun `test codeforces course`() {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val panel = CodeforcesPlatformProvider().createPanel(scope, testRootDisposable)
    val course = course(courseProducer = ::CodeforcesCourse) { }
    val coursePanel = panel.coursePanel
    coursePanel.bindCourse(course)

    val startButton = coursePanel.buttonsPanel.buttons.first()
    assertTrue(startButton.isVisible)
    assertTrue(startButton.isEnabled)
  }

  fun `test course preview`() {
    val course = course { }
    val coursePanel = CCCreateCoursePreviewDialog(myFixture.project, course, PlainTextConfigurator()).panel
    val startButton = coursePanel.buttonsPanel.buttons.first()
    assertTrue(startButton.isVisible)
    assertTrue(startButton.isEnabled)
  }
}