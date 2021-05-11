package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val LEFT_RIGHT_OFFSET = 13

private const val TOP_LOGIN_OFFSET = 17
private const val BOTTOM_LOGIN_OFFSET = 5

private const val TOP_BOTTOM_INFO_OFFSET = 13

class TabInfoPanel(tabInfo: TabInfo) : NonOpaquePanel() {
  private val infoText: String
  private val loginComponent: JPanel?

  init {
    layout = BorderLayout()

    infoText = tabInfo.description
    loginComponent = tabInfo.loginComponent

    if (loginComponent != null) {
      loginComponent.border = JBUI.Borders.empty(TOP_LOGIN_OFFSET, LEFT_RIGHT_OFFSET, BOTTOM_LOGIN_OFFSET, LEFT_RIGHT_OFFSET)
      add(loginComponent, BorderLayout.NORTH)
    }

    val infoPanel = GrayTextHtmlPanel(infoText)
    infoPanel.border = JBUI.Borders.empty(TOP_BOTTOM_INFO_OFFSET, LEFT_RIGHT_OFFSET)
    val scrollPane = JBScrollPane(infoPanel).apply {
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      border = null
    }
    add(scrollPane, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(this, MAIN_BG_COLOR)
  }

  fun hideLoginPanel() {
    loginComponent?.isVisible = false
  }

}

open class LoginPanel(isVisible: Boolean, beforeLinkText: String, linkText: String, loginHandler: () -> Unit) : JPanel(BorderLayout()) {

  init {
    val hyperlinkLabel = HyperlinkLabel().apply {
      setHyperlinkText("$beforeLinkText ", linkText, "")
      addHyperlinkListener { loginHandler() }
      setIcon(AllIcons.General.BalloonInformation)
      foreground = beforeLinkForeground
      font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.fontSize)
      iconTextGap = 5
    }

    this.add(hyperlinkLabel, BorderLayout.CENTER)
    this.isVisible = isVisible
  }

  open val beforeLinkForeground: Color
    get() = UIUtil.getLabelForeground()
}

class TabInfo(val description: String, val loginComponent: LoginPanel? = null)

class LinkInfo(val text: String, val url: String)