package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

class JetBrainsAcademyCoursesPanel(
  private val platformProvider: JetBrainsAcademyPlatformProvider, scope: CoroutineScope, disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="${JBA_HELP}">${EduNames.JBA}</a>"""
    val infoText = EduCoreBundle.message("hyperskill.courses.explanation", linkText)
    return TabInfo(infoText, JetBrainsAcademyLoginPanel())
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: Course) {
    if (deletedCourse is CourseMetaInfo) {
      if (coursesGroups.isNotEmpty()) {
        val coursesGroup = coursesGroups.first()

        coursesGroup.courses = coursesGroup.courses.filter { it != deletedCourse }

        if (coursesGroup.courses.isEmpty()) {
          coursesGroup.courses = listOf(JetBrainsAcademyCourse())
        }
      }
    }

    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = JetBrainsAcademyCoursesListPanel()

  inner class JetBrainsAcademyCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return JetBrainsAcademyCourseCard(course)
    }
  }

  private inner class JetBrainsAcademyLoginPanel : LoginPanel(isLoginNeeded(),
                                                              EduCoreBundle.message("course.dialog.jba.log.in.label.before.link"),
                                                              EduCoreBundle.message("course.dialog.log.in.to", EduNames.JBA),
                                                              { handleLogin() }) {
    override val beforeLinkForeground: Color
      get() = EduColors.warningTextForeground
  }

  override fun isLoginNeeded() = HyperskillSettings.INSTANCE.account == null

  private fun handleLogin() {
    HyperskillConnector.getInstance().doAuthorize(
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { setButtonsEnabled(true) },
      Runnable { hideLoginPanel() },
      Runnable { scheduleUpdateAfterLogin() }
    )
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val academyCoursesGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
    coursesGroups.clear()
    coursesGroups.addAll(academyCoursesGroups)
    super.updateCoursesAfterLogin(false)
  }
}