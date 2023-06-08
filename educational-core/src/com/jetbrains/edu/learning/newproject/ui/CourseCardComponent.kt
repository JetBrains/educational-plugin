package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val CARD_GAP = 10
private const val CARD_WIDTH = 80
private const val CARD_HEIGHT = 70
private const val LOGO_SIZE = 40

private val HOVER_COLOR: Color = JBColor.namedColor("SelectCourse.CourseCard.hoverBackground", 0xF5F9FF, 0x282A2C)
private val SELECTION_COLOR: Color = JBColor.namedColor("SelectCourse.CourseCard.hoverBackground", 0xE9EEF5, 0x36393B)
val GRAY_COLOR: Color = JBColor.namedColor("SelectCourse.grayForeground", JBColor(Gray._120, Gray._135))

open class CourseCardComponent(val course: Course) : JPanel(BorderLayout()) {
  private val logoComponent: JLabel = JLabel()
  private val courseNameInfoComponent: JPanel
  lateinit var actionComponent: JComponent
  protected lateinit var baseComponent: JComponent

  init {
    border = JBUI.Borders.empty(CARD_GAP)
    preferredSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)

    logoComponent.isOpaque = false
    logoComponent.icon = course.getScaledLogo(JBUI.scale(LOGO_SIZE), this)
    logoComponent.border = JBUI.Borders.emptyRight(CARD_GAP)
    @Suppress("LeakingThis")
    logoComponent.isVisible = isLogoVisible()
    this.add(logoComponent, BorderLayout.LINE_START)

    courseNameInfoComponent = this.createMainComponent()
    this.add(courseNameInfoComponent, BorderLayout.CENTER)

    updateColors(false)
  }

  open fun isLogoVisible() = true

  open fun getClickComponent(): Component {
    return courseNameInfoComponent
  }

  private fun createMainComponent(): JPanel {
    baseComponent = NonOpaquePanel()
    baseComponent.add(CourseNameComponent(course), BorderLayout.NORTH)
    baseComponent.add(this.createBottomComponent(), BorderLayout.SOUTH)

    val panel = NonOpaquePanel()
    panel.add(baseComponent, BorderLayout.CENTER)

    actionComponent = createSideActionComponent()
    panel.add(actionComponent, BorderLayout.LINE_END)

    return panel
  }

  protected open fun createSideActionComponent(): JComponent {
    return JPanel()
  }

  protected open fun createBottomComponent(): JComponent {
    return JPanel()
  }

  protected fun setActionComponentVisible(visible: Boolean) {
    actionComponent.isVisible = visible
    actionComponent.isEnabled = visible
  }

  fun updateColors(isSelected: Boolean) {
    updateColors(if (isSelected) SELECTION_COLOR else SelectCourseBackgroundColor)
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

  open fun onHover(isSelected: Boolean) {
    if (!isSelected) {
      updateColors(HOVER_COLOR)
    }
  }

  open fun onHoverEnded() {}

  private fun scrollToVisible() {
    val parent = parent as JComponent
    val bounds = bounds
    if (!parent.visibleRect.contains(bounds)) {
      parent.scrollRectToVisible(bounds)
    }
  }

}

private class CourseNameComponent(course: Course) : JPanel(BorderLayout()) {

  init {
    val nameLabel = JLabel().apply {
      text = course.name
      font = Font(TypographyManager().bodyFont, Font.BOLD, CoursesDialogFontManager.fontSize)
    }

    add(nameLabel, BorderLayout.CENTER)
  }
}