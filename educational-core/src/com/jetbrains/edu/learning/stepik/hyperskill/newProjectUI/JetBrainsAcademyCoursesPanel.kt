package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

class JetBrainsAcademyCoursesPanel(
  private val platformProvider: JetBrainsAcademyPlatformProvider, scope: CoroutineScope
) : CoursesPanel(platformProvider, scope) {

  override fun tabInfo(): TabInfo {
    val infoText = EduCoreBundle.message("hyperskill.courses.explanation", EduNames.JBA)
    val linkText = EduCoreBundle.message("course.dialog.go.to.website")
    val linkInfo = LinkInfo(linkText, HYPERSKILL_DEFAULT_URL)
    return TabInfo(infoText, linkInfo, JetBrainsAcademyLoginPanel())
  }

  override fun createCourseCard(course: Course): CourseCardComponent {
    return JetBrainsAcademyCourseCard(course)
  }

  private inner class JetBrainsAcademyLoginPanel : LoginPanel(isLoginNeeded(),
                                                              EduCoreBundle.message("course.dialog.jba.log.in.label.before.link"),
                                                              EduCoreBundle.message("course.dialog.log.in.to", EduNames.JBA),
                                                              { handleLogin() })  {
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