package com.jetbrains.edu.learning.stepik.hyperskill.settings

import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillSupportAvailable
import com.jetbrains.edu.learning.stepik.hyperskill.profileUrl
import javax.swing.JComponent

class HyperskillOptions : OAuthLoginOptions<HyperskillAccount>() {
  private var automaticUpdateCheckBox: JBCheckBox = JBCheckBox(EduCoreBundle.message("hyperskill.settings.auto.update"),
                                                               HyperskillSettings.INSTANCE.updateAutomatically)

  override val connector: EduOAuthCodeFlowConnector<HyperskillAccount, *>
    get() = HyperskillConnector.getInstance()

  override fun getDisplayName(): String = EduNames.JBA

  override fun isAvailable(): Boolean = isHyperskillSupportAvailable()

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun getAdditionalComponents(): List<JComponent> {
    return listOf(automaticUpdateCheckBox)
  }

  override fun apply() {
    super.apply()
    HyperskillSettings.INSTANCE.updateAutomatically = automaticUpdateCheckBox.isSelected
  }

  override fun reset() {
    super.reset()
    automaticUpdateCheckBox.isSelected = HyperskillSettings.INSTANCE.updateAutomatically
  }

  override fun isModified(): Boolean {
    return super.isModified() || HyperskillSettings.INSTANCE.updateAutomatically != automaticUpdateCheckBox.isSelected
  }
}
