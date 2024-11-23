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
      link(EduCoreBundle.message("user.agreement.dialog.checkbox.agreement")) { EduBrowser.getInstance().browse(USER_AGREEMENT_URL) }
        .resizableColumn()
        .customize(leftGap())
      label(EduCoreBundle.message("user.agreement.dialog.checkbox.and"))
        .customize(leftGap())
      link(EduCoreBundle.message("user.agreement.dialog.checkbox.privacy.policy")) { EduBrowser.getInstance().browse(PRIVACY_POLICY_URL) }
        .resizableColumn()
        .customize(leftGap())
    }
  }

  fun createAiAgreementCheckBoxTextPanel(): JPanel = panel {
    row {
      text(
        EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text"),
        action = {
          EduBrowser.getInstance().browse(AI_TERMS_OF_USE_URL)
        })
        .customize(leftGap())
    }
  }

  private fun leftGap() = UnscaledGaps(0, 3, 0, 0)

  private const val AI_TERMS_OF_USE_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-ai-service/"
  private const val USER_AGREEMENT_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-academy/plugin/"
  private const val PRIVACY_POLICY_URL: String = "https://www.jetbrains.com/legal/docs/privacy/privacy/"

  const val EMPTY_TEXT: String = ""
}