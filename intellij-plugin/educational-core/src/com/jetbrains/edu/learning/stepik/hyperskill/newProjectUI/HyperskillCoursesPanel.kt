package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBCardLayout
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.HyperskillNotLoggedInPanel
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import kotlinx.coroutines.CoroutineScope
import javax.swing.JPanel

class HyperskillCoursesPanel(
  platformProvider: HyperskillPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {


  override fun tabDescription(): String {
    val linkText = """<a href="${JBA_HELP}">${EduNames.JBA}</a>"""
    return EduCoreBundle.message("hyperskill.courses.explanation", linkText)
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    if (coursesGroups.isNotEmpty()) {
      val coursesGroup = coursesGroups.first()

      coursesGroup.courses = coursesGroup.courses.filter { it.id != deletedCourse.id }

      if (coursesGroup.courses.isEmpty()) {
        coursesGroup.courses = listOf(HyperskillCourseAdvertiser())
      }
    }

    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = HyperskillCoursesListPanel()

  override fun createContentPanel(): JPanel {
    val jbCardLayout = JBCardLayout()
    val coursesPageId = "COURSES PAGE"
    val loginPageId = "LOGIN PAGE"
    val panel = JPanel(jbCardLayout)
    panel.add(loginPageId, HyperskillNotLoggedInPanel())
    panel.add(coursesPageId, super.createContentPanel())
    if (isLoggedIn()) {
      jbCardLayout.show(panel, coursesPageId)
    }
    else {
      jbCardLayout.show(panel, loginPageId)
    }

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(HyperskillSettings.LOGGED_IN_TO_HYPERSKILL,
      object : EduLogInListener {
        override fun userLoggedIn() {

          jbCardLayout.show(panel, coursesPageId)

          connection.disconnect()
        }
      }
    )

    return panel
  }

  private fun isLoggedIn() = HyperskillSettings.INSTANCE.account != null

  inner class HyperskillCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return HyperskillCourseCard(course)
    }
  }
}