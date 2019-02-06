package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider

class HyperskillWidgetProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project): StatusBarWidget = HyperskillWidget()
  override fun getAnchor(): String  = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)
}
