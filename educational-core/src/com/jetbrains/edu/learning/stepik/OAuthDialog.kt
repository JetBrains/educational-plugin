package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import javax.swing.Action
import javax.swing.JComponent

class OAuthDialog : DialogWrapper(false) {
  private val loginPanel: AuthorizationPanel = AuthorizationPanel()

  init {
    title = EduCoreBundle.message("authorization.title", StepikNames.STEPIK)
    init()
  }

  override fun createActions(): Array<Action> = arrayOf(okAction, cancelAction)

  override fun createCenterPanel(): JComponent? = loginPanel.contentPanel

  override fun getPreferredFocusedComponent(): JComponent? = loginPanel.preferableFocusComponent

  override fun doOKAction() {
    val code = loginPanel.code.nullize() ?: return

    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        val success = EduUtils.execCancelable {
          StepikConnector.getInstance().login(code, StepikNames.EXTERNAL_REDIRECT_URL)
        }
        if (success == true) {
          doJustOkAction()
        }
        else {
          setError()
        }
      }, EduCoreBundle.message("authorizing.on.title", StepikNames.STEPIK), true, null)
  }

  private fun doJustOkAction() {
    ApplicationManager.getApplication().invokeLater { super.doOKAction() }
  }

  private fun setError() {
    ApplicationManager.getApplication().invokeLater {
      setErrorText(EduCoreBundle.message("error.login.failed"))
    }
  }
}