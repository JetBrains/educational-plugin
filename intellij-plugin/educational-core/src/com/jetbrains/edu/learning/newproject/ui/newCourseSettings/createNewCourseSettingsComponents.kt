package com.jetbrains.edu.learning.newproject.ui.newCourseSettings

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import java.awt.BorderLayout
import javax.swing.JComponent

@RequiresEdt
internal fun <S : NewCourseSettings> createNewCourseSettingsComponents(
  newCourseSettingsUI: NewCourseSettingsUI<S>, @RequiresEdt setNewCourseSettings: (S) -> Unit
): List<LabeledComponent<JComponent>> {
  return when (newCourseSettingsUI) {
    is NewCourseSettingsUI.List -> {
      val (catalog, presenter) = newCourseSettingsUI

      val newCourseSettingsComboBox = NewCourseSettingsComboBox(catalog, presenter)

      newCourseSettingsComboBox.addActionListener {
        setNewCourseSettings(newCourseSettingsComboBox.selectedSettings)
      }
      listOf(LabeledComponent.create(newCourseSettingsComboBox, presenter.label, BorderLayout.WEST))
    }

    is NewCourseSettingsUI.NoSettings -> emptyList()
  }
}