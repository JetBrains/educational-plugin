package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import javax.swing.JComponent

class LoginDialog : DialogWrapper(false) {
  val loginField = JBTextField()
  val passwordField = JBPasswordField()
  private val fieldSize = JBUI.size(350, 15)

  init {
    title = EduCoreBundle.message("dialog.title.login.to", CodeforcesNames.CODEFORCES_TITLE)
    setOKButtonText(EduCoreBundle.message("course.dialog.button.login"))
    loginField.emptyText.text = EduCoreBundle.message("label.enter.handle.or.email")
    loginField.preferredSize = fieldSize
    passwordField.preferredSize = fieldSize
    init()
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return loginField
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("${EduCoreBundle.message("label.handle.email")}:") {
        loginField()
      }
      row("${EduCoreBundle.message("label.password")}:") {
        passwordField()
      }
    }
  }

  override fun doOKAction() {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        val success = EduUtils.execCancelable {
          CodeforcesConnector.getInstance().login(loginField.text, String(passwordField.password)).onError {
            setErrorText(it)
            false
          }
        }
        if (success != null && success) {
          ApplicationManager.getApplication().invokeLater { super.doOKAction() }
        }
      }, "Authorizing on Codeforces...", true, null)
  }
}
