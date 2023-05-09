package com.jetbrains.edu.learning.codeforces.authorization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import javax.swing.JComponent

class LoginDialog(private val authorizationPlace: AuthorizationPlace) : DialogWrapper(false) {
  private val loginField = JBTextField()
  private val passwordField = JBPasswordField()

  init {
    title = EduCoreBundle.message("dialog.title.login.to", CodeforcesNames.CODEFORCES_TITLE)
    setOKButtonText(EduCoreBundle.message("course.dialog.button.login"))
    loginField.emptyText.text = EduCoreBundle.message("label.enter.handle.or.email")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("${EduCoreBundle.message("label.handle.email")}:") {
        // BACKCOMPAT: 2022.2. Use `align(AlignX.FILL)` instead of `horizontalAlign(HorizontalAlign.FILL)`
        @Suppress("UnstableApiUsage", "DEPRECATION")
        cell(loginField)
          .columns(COLUMNS_MEDIUM)
          .horizontalAlign(HorizontalAlign.FILL)
          .focused()
      }
      row("${EduCoreBundle.message("label.password")}:") {
        // BACKCOMPAT: 2022.2. Use `align(AlignX.FILL)` instead of `horizontalAlign(HorizontalAlign.FILL)`
        @Suppress("UnstableApiUsage", "DEPRECATION")
        cell(passwordField)
          .columns(COLUMNS_MEDIUM)
          // Only `.columns(COLUMNS_MEDIUM)` doesn't produce the same width of password and login fields on Windows.
          // Most likely, it's because of different dot symbol width on different OS
          .horizontalAlign(HorizontalAlign.FILL)
      }
    }
  }

  override fun doOKAction() {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        val account = EduUtils.execCancelable {
          CodeforcesConnector.getInstance().login(loginField.text, String(passwordField.password)).onError {
            setErrorText(it)
            null
          }
        }
        if (account != null) {
          CodeforcesSettings.getInstance().setAccountWithStatisticsEvent(account, authorizationPlace)
          ApplicationManager.getApplication().invokeLater { super.doOKAction() }
        }
      }, EduCoreBundle.message("codeforces.authorizing.on.codeforces"), true, null)
  }
}
