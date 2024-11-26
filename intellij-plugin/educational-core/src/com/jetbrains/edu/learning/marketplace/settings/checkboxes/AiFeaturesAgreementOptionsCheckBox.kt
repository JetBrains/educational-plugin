package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiFeaturesAgreementOptionsCheckBox : MarketplaceOptionsCheckBox(EduCoreBundle.message("marketplace.options.ai.features.checkbox")) {

  init {
    update()
  }

  override fun update() {
    val lastStatisticsState = MarketplaceSettings.INSTANCE.aiFeaturesAgreement
    updateCheckbox(lastStatisticsState, false)
    MarketplaceSettings.INSTANCE.updateToActualAiFeaturesAgreementState {
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
