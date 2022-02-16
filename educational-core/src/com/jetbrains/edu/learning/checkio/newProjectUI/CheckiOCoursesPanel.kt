package com.jetbrains.edu.learning.checkio.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_HELP
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
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

  override fun getLoginComponent(disposable: Disposable): LoginPanel = CheckiOLoginPanel(disposable)

  override fun tabDescription(): String {
    val linkText = """<a href="$CHECKIO_HELP">${CheckiONames.CHECKIO}</a>"""
    return EduCoreBundle.message("checkio.courses.explanation", linkText, EduNames.PYTHON, EduNames.JAVASCRIPT)
  }

  private inner class CheckiOLoginPanel(private val disposable: Disposable) : LoginPanel(
    true,
    CheckiONames.CHECKIO,
    EduCoreBundle.message("course.dialog.log.in.label.before.link")
  ) {
    override fun handleLogin() {
      val checkiOConnectorProvider = selectedCourse?.configurator as? CheckiOConnectorProvider ?: return
      val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
      checkiOOAuthConnector.subscribe(object : EduLogInListener {
        override fun userLoggedIn() {
          runInEdt(ModalityState.any()) {
            coursePanel.hideErrorPanel()
            hideLoginPanel()
            doValidation()
          }
        }
      }, disposable)
      checkiOOAuthConnector.doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
    }
  }
}
