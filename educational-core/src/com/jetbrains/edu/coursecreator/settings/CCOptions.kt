package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction.Companion.isCourseCreatorFeaturesEnabled
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRadioButton

class CCOptions : OptionsProvider {
  private val myHtmlRadioButton: JRadioButton? = null
  private val myMarkdownRadioButton: JRadioButton? = null
  private val myPanel: JPanel? = null
  private val myCustomOptions: JPanel? = null
  private val myShowSplitEditorCheckBox = JBCheckBox(null, CCSettings.getInstance().showSplitEditor())

  override fun createComponent(): JComponent? {
    if (!isCourseCreatorFeaturesEnabled) return null
    if (CCSettings.getInstance().useHtmlAsDefaultTaskFormat()) {
      myHtmlRadioButton!!.isSelected = true
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
        IdeFocusManager.getGlobalInstance().requestFocus(myHtmlRadioButton, true)
      }
    }
    else {
      myMarkdownRadioButton!!.isSelected = true
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
        IdeFocusManager.getGlobalInstance().requestFocus(myMarkdownRadioButton, true)
      }
    }
    if (isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)) {
      val showSplitEditorComponent = LabeledComponent.create(
        myShowSplitEditorCheckBox, "Show previous task file in framework lessons", BorderLayout.WEST)
      myCustomOptions!!.add(showSplitEditorComponent)
    }
    return myPanel
  }

  override fun isModified(): Boolean {
    val settings = CCSettings.getInstance()
    return myHtmlRadioButton!!.isSelected != settings.useHtmlAsDefaultTaskFormat() ||
           myShowSplitEditorCheckBox.isSelected != settings.showSplitEditor()
  }

  override fun apply() {
    if (isModified) {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(myHtmlRadioButton!!.isSelected)
      CCSettings.getInstance().setShowSplitEditor(myShowSplitEditorCheckBox.isSelected)
    }
  }

  override fun reset() {
    val settings = CCSettings.getInstance()
    myHtmlRadioButton!!.isSelected = settings.useHtmlAsDefaultTaskFormat()
    myMarkdownRadioButton!!.isSelected = !settings.useHtmlAsDefaultTaskFormat()
    myShowSplitEditorCheckBox.isSelected = settings.showSplitEditor()
  }

  override fun disposeUIResources() {}

  @Nls
  override fun getDisplayName(): String {
    return "Course Creator options"
  }
}