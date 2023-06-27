package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.CourseMetaInfo
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JetBrainsAcademyCoursesPanel(
  private val platformProvider: JetBrainsAcademyPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabDescription(): String {
    val linkText = """<a href="${JBA_HELP}">${EduNames.JBA}</a>"""
    return EduCoreBundle.message("hyperskill.courses.explanation", linkText)
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: CourseMetaInfo) {
    if (coursesGroups.isNotEmpty()) {
      val coursesGroup = coursesGroups.first()

      coursesGroup.courses = coursesGroup.courses.filter { it.id != deletedCourse.id }

      if (coursesGroup.courses.isEmpty()) {
        coursesGroup.courses = listOf(JetBrainsAcademyCourse())
      }
    }

    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = JetBrainsAcademyCoursesListPanel()

  override fun getLoginComponent(): LoginPanel {
    return JetBrainsAcademyLoginPanel()
  }

  inner class JetBrainsAcademyCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return JetBrainsAcademyCourseCard(course)
    }
  }

  private inner class JetBrainsAcademyLoginPanel : LoginPanel(EduCoreBundle.message("course.dialog.log.in.to.jba.label.text"),
                                                              isLoginNeeded(),
                                                              { handleLogin() })

  override fun isLoginNeeded() = HyperskillSettings.INSTANCE.account == null

  private fun handleLogin() {
    HyperskillConnector.getInstance().doAuthorize(
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { setButtonsEnabled(true) },
      Runnable { hideLoginPanel() },
      Runnable { scheduleUpdateAfterLogin() },
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