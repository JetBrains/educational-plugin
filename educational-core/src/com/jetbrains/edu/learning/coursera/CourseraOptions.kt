package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

class CourseraOptions : BoundConfigurable(CourseraNames.COURSERA), OptionsProvider {

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row("${EduCoreBundle.message("label.coursera.email")}:") {
        textField()
          .columns(COLUMNS_MEDIUM)
          .bindText(CourseraSettings.getInstance()::email)
      }
    }
  }
}
