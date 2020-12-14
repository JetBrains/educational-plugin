package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val CARD_GAP = 10
private const val CARD_WIDTH = 80
private const val CARD_HEIGHT = 70
private const val LOGO_SIZE = 40

private val HOVER_COLOR: Color = JBColor.namedColor("BrowseCourses.hoverBackground", JBColor(0xF5F9FF, 0x282A2C))
private val SELECTION_COLOR: Color = JBColor.namedColor("BrowseCourses.lightSelectionBackground", JBColor(0xE9EEF5, 0x36393B))
val GRAY_COLOR: Color = JBColor.namedColor("BrowseCourses.infoForeground", JBColor(Gray._120, Gray._135))

open class CourseCardComponent(val course: Course) : JPanel(BorderLayout()) {
  private val logoComponent: JLabel = JLabel()

  init {
    border = JBUI.Borders.empty(CARD_GAP)
    logoComponent.isOpaque = false
    logoComponent.icon = course.getScaledLogo(JBUI.scale(LOGO_SIZE), this)
    logoComponent.border = JBUI.Borders.emptyRight(CARD_GAP)

    this.add(logoComponent, BorderLayout.LINE_START)
    this.add(createCourseNameInfoComponent(), BorderLayout.CENTER)

    preferredSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)

    updateColors(false)
  }

  fun createCourseNameInfoComponent(): JPanel {
    val panel = NonOpaquePanel()
    panel.add(CourseNameComponent(course), BorderLayout.NORTH)
    panel.add(this.createCourseInfoComponent(), BorderLayout.SOUTH)

    return panel
  }

  open fun createCourseInfoComponent(): JPanel {
    return JPanel()
  }

  fun updateColors(isSelected: Boolean) {
    updateColors(if (isSelected) SELECTION_COLOR else MAIN_BG_COLOR)
  }

  private fun updateColors(background: Color) {
    UIUtil.setBackgroundRecursively(this, background)
  }

  fun setSelection(isSelectedOrHover: Boolean, scroll: Boolean = false) {
    if (scroll) {
      scrollToVisible()
    }
    updateColors(isSelectedOrHover)
    repaint()
  }

  fun setHover() {
    updateColors(HOVER_COLOR)
  }

  private fun scrollToVisible() {
    val parent = parent as JComponent
    val bounds = bounds
    if (!parent.visibleRect.contains(bounds)) {
      parent.scrollRectToVisible(bounds)
    }
  }

}

private class CourseNameComponent(course: Course) : JPanel(BorderLayout()) {
  private val nameLabel: JLabel = JLabel()

  init {
    nameLabel.text = course.name
    nameLabel.font = Font(TypographyManager().bodyFont, Font.BOLD, CoursesDialogFontManager.fontSize)
    add(nameLabel, BorderLayout.CENTER)
  }
}