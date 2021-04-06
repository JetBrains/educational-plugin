package com.jetbrains.edu.learning.checkio.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_HELP
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import kotlinx.coroutines.CoroutineScope

class CheckiOCoursesPanel(platformProvider: CoursesPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {
  private val loginComponent = CheckiOLoginPanel()

  override fun processSelectionChanged() {
    super.processSelectionChanged()
    if (selectedCourse != null) {
      val checkiOConnectorProvider = selectedCourse?.configurator as? CheckiOConnectorProvider
      if (checkiOConnectorProvider == null) {
        loginComponent.isVisible = false
        return
      }

      val checkiOAccount = checkiOConnectorProvider.oAuthConnector.account
      val isLoggedIn = checkiOAccount == null
      loginComponent.isVisible = isLoggedIn
    }
  }

  override fun tabInfo(): TabInfo {
    val linkText = CheckiONames.CHECKIO
    val infoText = EduCoreBundle.message("checkio.courses.explanation", CheckiONames.CHECKIO, EduNames.PYTHON, EduNames.JAVASCRIPT).dropWhile { it in linkText }
    val linkInfo = LinkInfo(linkText, CHECKIO_HELP)
    return TabInfo(infoText, linkInfo, loginComponent)
  }

  private inner class CheckiOLoginPanel : LoginPanel(true,
                                                     EduCoreBundle.message("course.dialog.log.in.label.before.link"),
                                                     EduCoreBundle.message("course.dialog.log.in.to", CheckiONames.CHECKIO),
                                                     {
                                                       val checkiOConnectorProvider = (selectedCourse?.configurator as CheckiOConnectorProvider?)!!
                                                       val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
                                                       checkiOOAuthConnector.doAuthorize(
                                                         Runnable { coursePanel.hideErrorPanel() },
                                                         Runnable { coursePanel.hideErrorPanel() },
                                                         Runnable { doValidation() }
                                                       )
                                                     })
}
