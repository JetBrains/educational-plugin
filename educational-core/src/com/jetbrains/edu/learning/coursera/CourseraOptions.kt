package com.jetbrains.edu.learning.coursera

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.settings.OptionsProvider
import javax.swing.JComponent

class CourseraOptions : OptionsProvider {
  private val emailField = JBTextField()
  private val loginPanel = panel {
    row("Email:") { emailField(growPolicy = GrowPolicy.MEDIUM_TEXT) }
  }

  init {
    loginPanel.border = IdeBorderFactory.createTitledBorder(CourseraNames.COURSERA)
  }

  override fun isModified() = CourseraSettings.getInstance().email != emailField.text


  override fun getDisplayName() = CourseraNames.COURSERA

  override fun apply() {
    CourseraSettings.getInstance().email = emailField.text
  }

  override fun reset() {
    emailField.text = CourseraSettings.getInstance().email
  }

  override fun createComponent(): JComponent = loginPanel
}