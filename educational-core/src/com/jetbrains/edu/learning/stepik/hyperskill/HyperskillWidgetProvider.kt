package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.jetbrains.edu.learning.EduUtils

class HyperskillWidgetProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project): StatusBarWidget? {
    if (EduUtils.isStudyProject(project)) {
      return HyperskillWidget()
    }
    return null
  }

  override fun getAnchor(): String  = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)
}
