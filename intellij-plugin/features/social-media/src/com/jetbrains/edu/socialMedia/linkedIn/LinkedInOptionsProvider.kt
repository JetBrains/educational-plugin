package com.jetbrains.edu.socialMedia.linkedIn

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

class LinkedInOptionsProvider : BoundConfigurable(EduCoreBundle.message("linkedin.configurable.name")), OptionsProvider {

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        checkBox(EduCoreBundle.message("linkedin.ask.to.post"))
          .bindSelected(LinkedInSettings.getInstance()::askToPost)
      }
    }
  }
}
