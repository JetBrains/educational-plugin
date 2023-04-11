package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction.Companion.isCourseCreatorFeaturesEnabled
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import javax.swing.ButtonGroup
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
@NlsSafe
private const val HTML = "Html"

@Suppress("UnstableApiUsage")
@NlsSafe
private const val MARKDOWN = "Markdown"

class CCOptions : OptionsProvider {

  private val htmlRadioButton = JBRadioButton(HTML, CCSettings.getInstance().useHtmlAsDefaultTaskFormat())
  private val markdownRadioButton = JBRadioButton(MARKDOWN, !CCSettings.getInstance().useHtmlAsDefaultTaskFormat())

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
    val buttonGroup = ButtonGroup()
    buttonGroup.add(htmlRadioButton)
    buttonGroup.add(markdownRadioButton)
  }

  override fun getDisplayName(): String = EduCoreBundle.message("ccoptions.display.name")

  override fun createComponent(): JComponent? {
    if (!isCourseCreatorFeaturesEnabled) return null

    return panel {
      group(displayName) {
        row {
          label(EduCoreBundle.message("ccoptions.description.format"))
          cell(htmlRadioButton)
          cell(markdownRadioButton)
        }
        row { cell(copyTestsCheckBox) }
        if (isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)) {
          row { cell(showSplitEditorCheckBox) }
        }
      }
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
}
