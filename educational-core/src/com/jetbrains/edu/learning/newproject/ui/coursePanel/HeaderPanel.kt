package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val LOGO_SIZE = 80
private const val INITIAL_LOGO_SIZE = 16f
private const val ICON_TOP_OFFSET = 10
private const val HEADER_HGAP = 20
private const val TOP_OFFSET = 15

class HeaderPanel(leftMargin: Int, errorHandler: (ErrorState) -> Unit) : NonOpaquePanel() {
  private var nameAndInfoPanel = NameAndInfoPanel(errorHandler)
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
      iconLabel.icon = course.logo
      revalidate()
      repaint()
    }

    private val Course.logo: Icon?
      get() {
        val language = course.languageById
        val configurator = course.configurator
        if (configurator == null) {
          LOG.info(String.format("configurator is null, language: %s course type: %s", language.displayName, course.itemType))
          return null
        }
        val logo = configurator.logo
        val scaleFactor = LOGO_SIZE / INITIAL_LOGO_SIZE
        val scaledIcon = IconUtil.scale(logo, this@IconPanel, scaleFactor)
        return IconUtil.toSize(scaledIcon, JBUI.scale(LOGO_SIZE), JBUI.scale(LOGO_SIZE))
      }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(HeaderPanel::class.java)
  }
}

