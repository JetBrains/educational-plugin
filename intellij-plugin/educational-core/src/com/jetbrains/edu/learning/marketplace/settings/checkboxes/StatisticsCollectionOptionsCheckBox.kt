package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatisticsCollectionOptionsCheckBox : MarketplaceOptionsCheckBox(EduCoreBundle.message("marketplace.options.statistics.checkbox")) {

  init {
    update()
  }

  override fun update() {
    val lastStatisticsState = MarketplaceSettings.INSTANCE.statisticsCollectionState
    updateCheckbox(lastStatisticsState, false)

    MarketplaceSettings.INSTANCE.updateToActualStatisticsSharingState {
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        updateCheckbox(it)
      }
    }
  }

  private fun updateCheckbox(statisticsState: Boolean?, enabled: Boolean = statisticsState != null) {
    val isStatisticsCollectionAllowed = statisticsState ?: false
    isSelected = isStatisticsCollectionAllowed && MarketplaceSettings.isJBALoggedIn()
    isEnabled = enabled
  }
}
