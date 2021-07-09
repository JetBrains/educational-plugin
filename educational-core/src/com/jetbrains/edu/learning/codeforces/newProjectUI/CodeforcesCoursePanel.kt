package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.GrayTextHtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.*
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDetailsPanel.Companion.formatNumber
import java.awt.BorderLayout
import java.awt.Font
import java.time.format.DateTimeFormatter

class CodeforcesCoursePanel : CoursePanel(false) {

  init {
    buttonsPanel.border = JBUI.Borders.empty()
  }

  override fun addComponents() {
    with(content) {
      add(tagsPanel)
      add(titlePanel)
      add(authorsPanel)
      add(errorComponent)
      add(courseDetailsPanel)
      add(buttonsPanel)
      add(settingsPanel)
    }
  }

  override val startButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button.codeforces.practice")

  override val openButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button.codeforces.open.contest")

  override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
    CodeforcesPlatformProvider().joinAction(info, mode, this)
  }

  override fun createCourseDetailsPanel(): NonOpaquePanel {
    return ContestDetailsPanel()
  }
}

private class ContestDetailsPanel : NonOpaquePanel(), CourseSelectionListener {
  private val finishedLabel: JBLabel = JBLabel()
  private val participantsLabel: JBLabel = JBLabel()

  init {
    border = JBUI.Borders.empty(5, HORIZONTAL_MARGIN, 8, 0)

    val headersPanel = NonOpaquePanel().apply {
      layout = VerticalFlowLayout(0, 10)
      border = JBUI.Borders.emptyRight(18)
      add(createBoldLabel(EduCoreBundle.message("codeforces.course.selection.finished")))
      add(createBoldLabel(EduCoreBundle.message("codeforces.course.selection.participants")))
    }

    val valuePanel = NonOpaquePanel().apply {
      layout = VerticalFlowLayout(0, 10)
      add(finishedLabel)
      add(participantsLabel)
    }

    val grayTextHtmlPanel = GrayTextHtmlPanel(EduCoreBundle.message("codeforces.past.contest.description")).apply {
      border = JBUI.Borders.empty(8, 0, 0, 0)
    }

    add(headersPanel, BorderLayout.LINE_START)
    add(valuePanel, BorderLayout.CENTER)
    add(grayTextHtmlPanel, BorderLayout.PAGE_END)
  }

  private fun createBoldLabel(text: String) = JBLabel(text).apply {
    font = font.deriveFont(Font.BOLD, 13.0f)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val codeforcesCourse = courseInfo.course as CodeforcesCourse
    val date = codeforcesCourse.endDateTime?.format(DateTimeFormatter.ofPattern(CourseDetailsPanel.DATE_PATTERN))
    finishedLabel.text = date
    participantsLabel.text = formatNumber(codeforcesCourse.participantsNumber)
  }

}