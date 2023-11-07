package com.jetbrains.edu.learning.checkio.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_HELP
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECKIO
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.platformProviders.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursesList.LoginPanel
import kotlinx.coroutines.CoroutineScope

class CheckiOCoursesPanel(
  platformProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun processSelectionChanged() {
    super.processSelectionChanged()
    if (selectedCourse != null) {
      val checkiOConnectorProvider = selectedCourse?.configurator as? CheckiOConnectorProvider
      if (checkiOConnectorProvider == null) {
        hideLoginPanel()
        return
      }

      if (checkiOConnectorProvider.oAuthConnector.isLoggedIn()) {
        hideLoginPanel()
      }
      else {
        showLoginPanel()
      }
    }
  }

  override fun isLoginNeeded(): Boolean = true

  override fun getLoginComponent(): LoginPanel {
    return CheckiOLoginPanel()
  }

  override fun tabDescription(): String {
    val linkText = """<a href="$CHECKIO_HELP">${CHECKIO}</a>"""
    return EduCoreBundle.message("checkio.courses.explanation", linkText, EduFormatNames.PYTHON, EduFormatNames.JAVASCRIPT)
  }

  private inner class CheckiOLoginPanel : LoginPanel(EduCoreBundle.message("course.dialog.log.in.to.checkio.label.text"),
                                                     true,
                                                     {
                                                       val checkiOConnectorProvider = (selectedCourse?.configurator as CheckiOConnectorProvider?)!!
                                                       val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
                                                       checkiOOAuthConnector.doAuthorize(
                                                         Runnable { coursePanel.hideErrorPanel() },
                                                         Runnable { hideLoginPanel() },
                                                         Runnable { doValidation() },
                                                         authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG
                                                       )
                                                     })
}
