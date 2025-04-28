package com.jetbrains.edu.socialMedia.linkedIn

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.settings.OptionsProvider
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle

class LinkedInOptionsProvider : BoundConfigurable(EduSocialMediaBundle.message("linkedin.configurable.name")), OptionsProvider {

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        checkBox(EduSocialMediaBundle.message("linkedin.ask.to.post"))
          .bindSelected(LinkedInSettings.getInstance()::askToPost)
      }
    }
  }
}
