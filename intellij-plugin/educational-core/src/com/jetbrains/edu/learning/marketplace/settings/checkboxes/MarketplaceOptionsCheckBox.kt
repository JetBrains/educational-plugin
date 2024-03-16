package com.jetbrains.edu.learning.marketplace.settings.checkboxes

import com.intellij.ui.components.JBCheckBox

sealed class MarketplaceOptionsCheckBox(text: String) : JBCheckBox(text) {

  abstract fun update()
}
