package com.jetbrains.edu.coursecreator.settings

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction.Companion.isCourseCreatorFeaturesEnabled
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import javax.swing.ButtonGroup
import javax.swing.JComponent

class CCOptions : OptionsProvider {

  private val htmlRadioButton = JBRadioButton("Html", CCSettings.getInstance().useHtmlAsDefaultTaskFormat())
  private val markdownRadioButton = JBRadioButton("Markdown", !CCSettings.getInstance().useHtmlAsDefaultTaskFormat())

  private val copyTestsCheckBox = JBCheckBox(
    EduCoreBundle.message("ccoptions.copy.tests"),
    CCSettings.getInstance().copyTestsInFrameworkLessons()
  ).apply {
    toolTipText = EduCoreBundle.message("ccoptions.copy.tests.tooltip")
  }

  private val showSplitEditorCheckBox = JBCheckBox(
    EduCoreBundle.message("ccoptions.split.editor"),
    CCSettings.getInstance().showSplitEditor()
  )

  init {
    // BACKCOMPAT: 2019.1. use radio button dsl
    //  https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html#radio-buttons
    val buttonGroup = ButtonGroup()
    buttonGroup.add(htmlRadioButton)
    buttonGroup.add(markdownRadioButton)
  }

  override fun getDisplayName(): String = EduCoreBundle.message("ccoptions.display.name")

  override fun createComponent(): JComponent? {
    if (!isCourseCreatorFeaturesEnabled) return null

    return panel {
      row(EduCoreBundle.message("ccoptions.task.description.format")) { }
      row { htmlRadioButton(gapLeft = RADIO_BUTTON_INDENT) }
      row { markdownRadioButton(gapLeft = RADIO_BUTTON_INDENT) }
      row { copyTestsCheckBox() }
      if (isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)) {
        row { showSplitEditorCheckBox() }
      }
    }.apply {
      border = IdeBorderFactory.createTitledBorder(displayName)
    }
  }

  override fun isModified(): Boolean {
    val settings = CCSettings.getInstance()
    return htmlRadioButton.isSelected != settings.useHtmlAsDefaultTaskFormat() ||
           showSplitEditorCheckBox.isSelected != settings.showSplitEditor() ||
           copyTestsCheckBox.isSelected != settings.copyTestsInFrameworkLessons()
  }

  override fun apply() {
    val settings = CCSettings.getInstance()
    settings.setUseHtmlAsDefaultTaskFormat(htmlRadioButton.isSelected)
    settings.setShowSplitEditor(showSplitEditorCheckBox.isSelected)
    settings.setCopyTestsInFrameworkLessons(copyTestsCheckBox.isSelected)
  }

  override fun reset() {
    val settings = CCSettings.getInstance()
    htmlRadioButton.isSelected = settings.useHtmlAsDefaultTaskFormat()
    markdownRadioButton.isSelected = !settings.useHtmlAsDefaultTaskFormat()
    showSplitEditorCheckBox.isSelected = settings.showSplitEditor()
    copyTestsCheckBox.isSelected = settings.copyTestsInFrameworkLessons()
  }

  companion object {
    private val RADIO_BUTTON_INDENT = JBUI.scale(24)
  }
}
