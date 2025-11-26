package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

class CourseraOptions : BoundConfigurable(COURSERA), OptionsProvider {
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
