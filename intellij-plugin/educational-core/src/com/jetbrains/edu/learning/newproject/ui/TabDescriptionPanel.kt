package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

private const val INFO_OFFSET = 13

class TabDescriptionPanel(tabDescription: String) : NonOpaquePanel() {

  init {
    layout = BorderLayout()

    val infoPanel = GrayTextHtmlPanel(tabDescription)
    infoPanel.border = JBUI.Borders.empty(INFO_OFFSET)
    val scrollPane = JBScrollPane(infoPanel).apply {
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      border = JBUI.Borders.empty()
    }
    add(scrollPane, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(this, SelectCourseBackgroundColor)
  }
}

