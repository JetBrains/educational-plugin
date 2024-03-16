package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserAgreementOptionsCheckBox : MarketplaceOptionsCheckBox(EduCoreBundle.message("marketplace.options.user.agreement.checkbox")) {

  init {
    update()
  }

  override fun update() {
    val lastAgreementState = MarketplaceSettings.INSTANCE.userAgreementState
    updateCheckbox(lastAgreementState, false)
    MarketplaceSettings.INSTANCE.updateToActualUserAgreementState {
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        updateCheckbox(it)
      }
    }
  }

  private fun updateCheckbox(agreementState: UserAgreementState?, enabled: Boolean = agreementState != null) {
    isSelected = agreementState == UserAgreementState.ACCEPTED && MarketplaceSettings.isJBALoggedIn()
    isEnabled = enabled
  }
}
