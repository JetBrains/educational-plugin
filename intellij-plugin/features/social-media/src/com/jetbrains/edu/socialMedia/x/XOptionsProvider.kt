package com.jetbrains.edu.socialMedia.x

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

class XOptionsProvider : BoundConfigurable(EduCoreBundle.message("x.configurable.name")), OptionsProvider {

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        checkBox(EduCoreBundle.message("x.ask.to.tweet"))
          .bindSelected(XSettings.getInstance()::askToPost)
      }
    }
  }
}
