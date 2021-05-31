package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout

private const val HEADER_HGAP = 20
private const val TOP_OFFSET = 15

class HeaderPanel(leftMargin: Int, joinCourseAction: (CourseInfo, CourseMode) -> Unit) : NonOpaquePanel() {
  private var nameAndInfoPanel: NameAndInfoPanel = NameAndInfoPanel(joinCourseAction)

  init {
    layout = BorderLayout(HEADER_HGAP, 0)
    border = JBUI.Borders.empty(TOP_OFFSET, leftMargin, 0, 0)
    add(nameAndInfoPanel, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(this, MAIN_BG_COLOR)
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    nameAndInfoPanel.setButtonsEnabled(isEnabled)
  }

  fun update(courseInfo: CourseInfo, settings: CourseDisplaySettings) {
    nameAndInfoPanel.update(courseInfo, settings)
  }

  fun setButtonToolTip(text: String?) {
    nameAndInfoPanel.setButtonToolTip(text)
  }

}

