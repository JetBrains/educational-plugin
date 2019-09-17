package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled

private const val ACTION_TEXT = "Change Hyperskill url"

@Suppress("ComponentNotRegistered")
class HyperskillChangeHost : DumbAwareAction(ACTION_TEXT), RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val initialValue = PropertiesComponent.getInstance().getValue(HYPERSKILL_URL_PROPERTY, HYPERSKILL_DEFAULT_URL)
    val result = Messages.showInputDialog("Enter Hyperskill url", "Enter Hyperskill url", null, initialValue, null)
    if (result != null) {
      PropertiesComponent.getInstance().setValue(HYPERSKILL_URL_PROPERTY, result)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.HYPERSKILL)
  }
}