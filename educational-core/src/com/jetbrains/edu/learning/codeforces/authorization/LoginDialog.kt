package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.credentialStore.Credentials
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class LoginDialog : DialogWrapper(false) {
  private val loginField = JBTextField()
  private val passwordField = JBPasswordField()

  var credentials: Credentials? = null
    private set

  init {
    title = EduCoreBundle.message("course.dialog.log.in.to.title", CodeforcesNames.CODEFORCES_TITLE)
    setOKButtonText(EduCoreBundle.message("course.dialog.button.login"))
    loginField.emptyText.text = "Enter Handle/Email"
    myPreferredFocusedComponent = loginField
    loginField.preferredSize = JBUI.size(350, 15)
    passwordField.preferredSize = JBUI.size(350, 15)
    init()
  }

  override fun createCenterPanel(): JComponent? {
    return panel {
      row("${EduCoreBundle.message("label.handle")}:") {
        loginField()
      }
      row("${EduCoreBundle.message("label.password")}:") {
        passwordField()
      }
    }
  }

  override fun getOKAction(): Action {
    return object : OkAction() {
      override fun doAction(e: ActionEvent) {
        credentials = Credentials(loginField.text, String(passwordField.password))
        super.doAction(e)
      }
    }
  }
}
