package com.jetbrains.edu.learning.courseSelection

import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.CheckiOPlatformProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider
import com.jetbrains.edu.learning.newproject.coursesStorage.JBCoursesStorage
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

class StartButtonsTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    JBCoursesStorage.getInstance().state.courses.clear()
  }

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
    val course = course {} as EduCourse
    course.init(course, false)
    val coursePanel = CCCreateCoursePreviewDialog(myFixture.project, course, PlainTextConfigurator()).panel
    val startButton = coursePanel.buttonsPanel.buttons.first()
    assertTrue(startButton.isVisible)
    assertTrue(startButton.isEnabled)
  }

  fun `test checkiO open button`() {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val panel = CheckiOPlatformProvider().createPanel(scope, testRootDisposable)
    val course = course { }
    JBCoursesStorage.getInstance().addCourse(course, "test")

    val coursePanel = panel.coursePanel
    coursePanel.bindCourse(course)

    val openButton = coursePanel.buttonsPanel.buttons[1]
    assertTrue(openButton.isVisible)
    assertTrue(openButton.isEnabled)
  }

  override fun tearDown() {
    super.tearDown()
    JBCoursesStorage.getInstance().state.courses.clear()
  }
}