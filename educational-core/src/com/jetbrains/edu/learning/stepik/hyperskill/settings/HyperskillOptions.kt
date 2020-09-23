package com.jetbrains.edu.learning.stepik.hyperskill.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OauthOptions
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROFILE_PATH
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProfileInfo
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillSupportAvailable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class HyperskillOptions : OauthOptions<HyperskillAccount>() {
  private var automaticUpdateCheckBox: JBCheckBox = JBCheckBox(EduCoreBundle.message("hyperskill.settings.auto.update"), HyperskillSettings.INSTANCE.updateAutomatically)

  override fun getCurrentAccount() : HyperskillAccount? = HyperskillSettings.INSTANCE.account

  override fun isAvailable(): Boolean = isHyperskillSupportAvailable()

  override fun setCurrentAccount(account: HyperskillAccount?) {
    HyperskillSettings.INSTANCE.account = account
    val messageBus = ApplicationManager.getApplication().messageBus
    if (account != null) {
      messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedIn()
    }
    else {
      messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()
    }
  }

  @Nls
  override fun getDisplayName(): String {
    return EduNames.JBA
  }

  override fun createAuthorizeListener(): LoginListener {
    return object : LoginListener() {
      override fun authorize(e: HyperlinkEvent?) {
        HyperskillConnector.getInstance().doAuthorize(Runnable {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        })
      }
    }
  }

  override fun getProfileUrl(userInfo: Any): String {
    val userId = try {
      (userInfo as HyperskillProfileInfo).id
    }
    catch (e: ClassCastException) {
      Logger.getInstance(HyperskillOptions::class.java).error(e.message)
      ""
    }

    return "${HYPERSKILL_PROFILE_PATH}${userId}"
  }

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
