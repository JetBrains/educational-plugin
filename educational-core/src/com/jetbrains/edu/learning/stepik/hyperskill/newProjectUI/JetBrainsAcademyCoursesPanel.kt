package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_URL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.Color

class JetBrainsAcademyCoursesPanel(
  dialog: BrowseCoursesDialog,
  platformProvider: JetBrainsAcademyPlatformProvider
) : CoursesPanel(dialog, platformProvider) {

  override fun tabInfo(): TabInfo? {
    val infoText = EduCoreBundle.message("hyperskill.courses.explanation", EduNames.JBA)
    val linkText = EduCoreBundle.message("course.dialog.go.to.website")
    val linkInfo = LinkInfo(linkText, HYPERSKILL_DEFAULT_URL)
    return TabInfo(infoText, linkInfo, JetBrainsAcademyLoginPanel())
  }

  private inner class JetBrainsAcademyLoginPanel : LoginPanel(!isLoggedIn(),
                                                              EduCoreBundle.message("course.dialog.jba.log.in.label.before.link"),
                                                              EduCoreBundle.message("course.dialog.log.in.to", EduNames.JBA),
                                                              { handleLogin() }) {
    override val beforeLinkForeground: Color
      get() = EduColors.warningTextForeground
  }

  private fun isLoggedIn() = HyperskillSettings.INSTANCE.account != null

  private fun handleLogin() {
    HyperskillConnector.getInstance().doAuthorize(
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { notifyListeners(true) },
      Runnable { coursesListPanel.hideLoginPanel() }
    )
  }
}