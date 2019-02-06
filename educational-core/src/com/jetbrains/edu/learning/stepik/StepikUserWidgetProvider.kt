package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider

class StepikUserWidgetProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project): StatusBarWidget = StepikUserWidget(project)
  override fun getAnchor(): String  = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)
}
