package com.jetbrains.edu.learning.agreement

import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JPanel

object UserAgreementUtil {
  @Suppress("DialogTitleCapitalization")
  fun createPluginAgreementCheckBoxTextPanel(): JPanel = panel {
    row {
      text(
        EduCoreBundle.message(
          "user.agreement.dialog.checkbox.agreement.text",
          USER_AGREEMENT_URL,
          PRIVACY_POLICY_URL
        )
      ) { EduBrowser.getInstance().browse(it.url) }
        .resizableColumn()
        .customize(leftGap())
    }
  }

  fun createAiAgreementCheckBoxTextPanel(): JPanel = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text", AI_TERMS_OF_USE_URL)) {
        EduBrowser.getInstance().browse(it.url)
      }.customize(leftGap())
    }
  }

  private fun leftGap() = UnscaledGaps(0, 3, 0, 0)

  private const val AI_TERMS_OF_USE_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-ai-service/"
  private const val USER_AGREEMENT_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-academy/plugin/"
  private const val PRIVACY_POLICY_URL: String = "https://www.jetbrains.com/legal/docs/privacy/privacy/"

  const val EMPTY_TEXT: String = ""
}