package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

class LoginDialog : DialogWrapper(false) {
  val loginField = JBTextField()
  val passwordField = JBPasswordField()

  init {
    title = EduCoreBundle.message("course.dialog.log.in.to.title", CodeforcesNames.CODEFORCES_TITLE)
    setOKButtonText(EduCoreBundle.message("course.dialog.button.login"))
    loginField.emptyText.text = EduCoreBundle.message("label.enter.handle.or.email")
    loginField.preferredSize = JBUI.size(350, 15)
    passwordField.preferredSize = JBUI.size(350, 15)
    init()
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return loginField
  }

  override fun createCenterPanel(): JComponent? {
    return panel {
      row("${EduCoreBundle.message("label.handle.email")}:") {
        loginField()
      }
      row("${EduCoreBundle.message("label.password")}:") {
        passwordField()
      }
    }
  }

}
