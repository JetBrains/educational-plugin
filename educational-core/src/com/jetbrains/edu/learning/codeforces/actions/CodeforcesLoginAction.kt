package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.authorization.LoginDialog
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CodeforcesLoginAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.codeforces.login")) {
  override fun actionPerformed(e: AnActionEvent) {
    var handle = ""
    while (true) {
      val loginDialog = LoginDialog(handle)
      if (loginDialog.showAndGet()) {
        handle = loginDialog.loginField.text
        if (CodeforcesConnector.getInstance().login(handle, String(loginDialog.passwordField.password))) {
          break
        }
      } else {
        break
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return
    if (project.course !is CodeforcesCourse) return

    presentation.isEnabledAndVisible = true
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Codeforces.CodeforcesLoginAction"
  }
}