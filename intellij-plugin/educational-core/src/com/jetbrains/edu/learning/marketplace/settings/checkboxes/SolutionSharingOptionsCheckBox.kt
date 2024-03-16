package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolutionSharingOptionsCheckBox : MarketplaceOptionsCheckBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox")) {

  init {
    update()
  }

  override fun update() {
    val settings = MarketplaceSettings.INSTANCE
    val solutionSharing = settings.solutionsSharing
    if (solutionSharing != null) {
      updateCheckBox(solutionSharing)
    }
    else {
      settings.updateSolutionSharingFromRemote { sharingPreference ->
        withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
          updateCheckBox(sharingPreference)
        }
      }
    }
  }

  private fun updateCheckBox(sharingPreference: Boolean?) {
    isSelected = sharingPreference ?: false
    isEnabled = sharingPreference != null && MarketplaceSettings.isJBALoggedIn()
  }
}
