package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import javax.swing.JEditorPane

object UserAgreementUtil {
  @Suppress("DialogTitleCapitalization")
  fun Row.pluginAgreementCheckBoxText(): Cell<JEditorPane> {
    return text(
      EduCoreBundle.message(
        "user.agreement.dialog.checkbox.agreement.text",
        USER_AGREEMENT_URL,
        PRIVACY_POLICY_URL
      )
    ) { EduBrowser.getInstance().browse(it.url) }
      .resizableColumn()
      .customize(leftGap())
  }

  fun Row.aiAgreementCheckBoxText(): Cell<JEditorPane> {
    return text(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text", AI_TERMS_OF_USE_URL)) {
      EduBrowser.getInstance().browse(it.url)
    }.resizableColumn().customize(leftGap())
  }

  fun showEnableSubmissionsDialog(project: Project): Boolean {
    val result = showYesNoDialog(
      EduCoreBundle.message("user.agreement.settings.title"),
      EduCoreBundle.message("marketplace.options.user.agreement.checkbox"),
      project
    )
    if (result) {
      SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn()
    }
    return result
  }

  private fun leftGap() = UnscaledGaps(0, 3, 0, 0)

  private const val AI_TERMS_OF_USE_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-ai-service/"
  private const val USER_AGREEMENT_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-academy/plugin/"
  private const val PRIVACY_POLICY_URL: String = "https://www.jetbrains.com/legal/docs/privacy/privacy/"
}