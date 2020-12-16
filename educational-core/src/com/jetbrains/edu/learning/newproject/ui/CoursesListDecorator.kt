package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val TOOLBAR_TOP_OFFSET = 10
private const val TOOLBAR_BOTTOM_OFFSET = 8
private const val TOOLBAR_LEFT_OFFSET = 13

class CoursesListDecorator(mainPanel: JPanel, tabInfo: TabInfo?, toolbarAction: ToolbarActionWrapper?) : NonOpaquePanel() {
  private var tabInfoPanel: TabInfoPanel? = null

  init {
    val listWithTabInfo = NonOpaquePanel()
    listWithTabInfo.add(mainPanel, BorderLayout.CENTER)

    if (tabInfo != null) {
      tabInfoPanel = TabInfoPanel(tabInfo).apply {
        listWithTabInfo.add(this, BorderLayout.NORTH)
      }
    }

    val scrollPane = JBScrollPane(listWithTabInfo,
                                  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scrollPane.apply {
      border = null
      background = MAIN_BG_COLOR
    }

    add(scrollPane, BorderLayout.CENTER)

    if (toolbarAction != null) {
      val toolbarPanel = createHyperlinkWithContextHelp(toolbarAction).apply {
        border = JBUI.Borders.empty(TOOLBAR_TOP_OFFSET, TOOLBAR_LEFT_OFFSET, TOOLBAR_BOTTOM_OFFSET, 0)
      }
      add(toolbarPanel, BorderLayout.SOUTH)
      scrollPane.border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 0, 0, 1, 0)
    }
  }

  fun hideLoginPanel() {
    tabInfoPanel?.hideLoginPanel()
  }
}