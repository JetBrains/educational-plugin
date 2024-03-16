package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolutionSharingCheckBox : MarketplaceCheckBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox")) {

  init {
    update()
  }

  override fun update() {
    val settings = MarketplaceSettings.INSTANCE
    val solutionSharing = settings.solutionsSharing
    val userAgreementState = settings.userAgreementState
    if (solutionSharing != null) {
      updateCheckBox(solutionSharing, userAgreementState)
    }
    else {
      settings.updateSolutionSharingFromRemote { sharingPreference ->
        withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
          updateCheckBox(sharingPreference, userAgreementState)
        }
      }
    }
  }

  private fun updateCheckBox(sharingPreference: Boolean?, userAgreementState: UserAgreementState?) {
    isSelected = sharingPreference ?: false
    isEnabled = sharingPreference != null && MarketplaceSettings.isJBALoggedIn() && userAgreementState == UserAgreementState.ACCEPTED
  }
}
