package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.getScaledLogo
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val ICON_TOP_OFFSET = 10
private const val HEADER_HGAP = 20
private const val TOP_OFFSET = 15
private const val LOGO_SIZE = 80

class HeaderPanel(
  leftMargin: Int,
  joinCourseAction: (CourseInfo, CourseMode) -> Unit
) : NonOpaquePanel() {
  private var nameAndInfoPanel = NameAndInfoPanel(joinCourseAction)
  private var iconPanel = IconPanel()

  init {
    layout = BorderLayout(HEADER_HGAP, 0)
    border = JBUI.Borders.empty(TOP_OFFSET, leftMargin, 0, 0)

    add(iconPanel.iconLabel, BorderLayout.WEST)
    add(nameAndInfoPanel, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(this, UIUtil.getEditorPaneBackground())
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    nameAndInfoPanel.setButtonsEnabled(isEnabled)
  }

  fun update(courseInfo: CourseInfo, settings: CourseDisplaySettings) {
    iconPanel.bind(courseInfo.course)
    nameAndInfoPanel.update(courseInfo, settings)
  }

  private class IconPanel : JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, false, false)) {
    var iconLabel = JBLabel()

    init {
      border = JBUI.Borders.emptyTop(ICON_TOP_OFFSET)

      iconLabel.verticalAlignment = SwingConstants.TOP
      iconLabel.isOpaque = false
      add(iconLabel)
    }

    fun bind(course: Course) {
      iconLabel.icon = course.getScaledLogo(LOGO_SIZE, this@IconPanel)
      revalidate()
      repaint()
    }
  }
}

