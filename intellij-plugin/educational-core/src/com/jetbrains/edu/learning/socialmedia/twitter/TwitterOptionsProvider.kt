package com.jetbrains.edu.learning.socialmedia.twitter

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

class TwitterOptionsProvider : BoundConfigurable(EduCoreBundle.message("twitter.configurable.name")), OptionsProvider {

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        checkBox(EduCoreBundle.message("twitter.ask.to.tweet"))
          .bindSelected(TwitterSettings.getInstance()::askToPost)
      }
    }
  }
}
