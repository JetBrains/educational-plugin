package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.layout.*
import javax.swing.JComponent

class CCMoveStudyItemDialog(project: Project, itemName: String, thresholdName: String) : DialogWrapper(project) {

  private val positionPanel: CCItemPositionPanel = CCItemPositionPanel(thresholdName)

  init {
    title = "Move ${StringUtil.toTitleCase(itemName)}"
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    positionPanel.attach(this)
  }

  val indexDelta: Int get() = positionPanel.indexDelta
}
