package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val TOOLBAR_TOP_OFFSET = 10
private const val TOOLBAR_BOTTOM_OFFSET = 8
private const val TOOLBAR_LEFT_OFFSET = 13
private const val TOP_LOGIN_OFFSET = 8
private const val BOTTOM_LOGIN_OFFSET = 10
private const val INFO_OFFSET = 13

class CoursesListDecorator(private val mainPanel: CoursesListPanel,
                           tabDescription: String?,
                           toolbarAction: ToolbarActionWrapper?) : NonOpaquePanel() {
  private var myTabDescriptionPanel: TabDescriptionPanel? = null

  init {
    val listWithTabInfo = NonOpaquePanel()
    listWithTabInfo.add(mainPanel, BorderLayout.CENTER)

    if (tabDescription != null) {
      myTabDescriptionPanel = TabDescriptionPanel(tabDescription).apply {
        listWithTabInfo.add(this, BorderLayout.NORTH)
      }
    }

    val scrollPane = JBScrollPane(listWithTabInfo,
                                  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scrollPane.apply {
      border = JBUI.Borders.empty()
      background = SelectCourseBackgroundColor
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

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    mainPanel.setSelectionListener(processSelectionChanged)
  }

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?) {
    mainPanel.updateModel(coursesGroups, courseToSelect)
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    mainPanel.setSelectedValue(newCourseToSelect)
  }

  fun getSelectedCourse() = mainPanel.selectedCourse
}

private class TabDescriptionPanel(tabDescription: String) : NonOpaquePanel() {

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
      border = JBUI.Borders.empty(TOP_LOGIN_OFFSET, TOOLBAR_LEFT_OFFSET, BOTTOM_LOGIN_OFFSET, TOOLBAR_LEFT_OFFSET)
    }
    @Suppress("LeakingThis")
    add(wrapper)
    border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 1, 0, 0, 0)
    background = BACKGROUND_COLOR
  }
}