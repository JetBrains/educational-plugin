package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.HyperskillNotLoggedInPanel
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JPanel

class HyperskillCoursesPanel(
  private val platformProvider: HyperskillPlatformProvider,
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
    return if (isLoggedIn()) {
      super.createContentPanel()
    }
    else {
      HyperskillNotLoggedInPanel { handleLogin() }
    }
  }

  private fun isLoggedIn() = HyperskillSettings.INSTANCE.account != null

  inner class HyperskillCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return HyperskillCourseCard(course)
    }
  }

  private fun handleLogin() {
    HyperskillConnector.getInstance().doAuthorize(
      Runnable {
        this.removeAll()
        this.add(super.createContentPanel())
      },
      authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG
    )
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val academyCoursesGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
    coursesGroups.clear()
    coursesGroups.addAll(academyCoursesGroups)
    super.updateCoursesAfterLogin(false)
  }
}