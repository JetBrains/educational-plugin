package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val LEFT_RIGHT_OFFSET = 13
private const val TOP_LOGIN_OFFSET = 8
private const val BOTTOM_LOGIN_OFFSET = 10
private const val TOP_BOTTOM_INFO_OFFSET = 13

class TabDescriptionPanel(tabDescription: String) : NonOpaquePanel() {

  init {
    layout = BorderLayout()

    val infoPanel = GrayTextHtmlPanel(tabDescription)
    infoPanel.border = JBUI.Borders.empty(TOP_BOTTOM_INFO_OFFSET, LEFT_RIGHT_OFFSET)
    val scrollPane = JBScrollPane(infoPanel).apply {
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      border = JBUI.Borders.empty()
    }
    add(scrollPane, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(this, SelectCourseBackgroundColor)
  }
}

open class LoginPanel(
  @Suppress("UnstableApiUsage") @NlsContexts.LinkLabel text: String,
  isVisible: Boolean,
  loginHandler: () -> Unit
) : JPanel(BorderLayout()) {
  private val BACKGROUND_COLOR = JBColor.namedColor("SelectCourse.LoginPanel.background", 0xE6EEF7, 0x1C3956)

  init {
    val hyperlinkLabel = HyperlinkLabel().apply {
      @Suppress("UnstableApiUsage")
      setTextWithHyperlink(text)
      addHyperlinkListener { loginHandler() }
      setIcon(AllIcons.General.BalloonInformation)
      font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.fontSize)
      iconTextGap = 5
    }
    val wrapper = NonOpaquePanel().apply {
      add(hyperlinkLabel, BorderLayout.CENTER)
      this.isVisible = isVisible
      border = JBUI.Borders.empty(TOP_LOGIN_OFFSET, LEFT_RIGHT_OFFSET, BOTTOM_LOGIN_OFFSET, LEFT_RIGHT_OFFSET)
    }
    @Suppress("LeakingThis")
    add(wrapper)
    border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 1, 0, 0, 0)
    background = BACKGROUND_COLOR
  }

  open val beforeLinkForeground: Color
    get() = UIUtil.getLabelForeground()
}