package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import icons.EducationalCoreIcons
import java.awt.BorderLayout
import javax.swing.JPanel

class FeedbackPanel(task: Task) : JPanel(BorderLayout()) {
  init {
    add(ResultLabel(task), BorderLayout.CENTER)
    val checkTime = task.feedback?.time?.time
    if (checkTime != null) {
      add(TimeLabel(checkTime), BorderLayout.EAST)
    }
  }

  private class ResultLabel(task: Task) : JBLabel() {
    init {
      val status = task.status

      iconTextGap = JBUI.scale(4)
      icon = when (status) {
        CheckStatus.Failed -> AllIcons.General.BalloonError
        CheckStatus.Solved -> EducationalCoreIcons.ResultCorrect
        else -> null
      }
      foreground = when (status) {
        CheckStatus.Failed -> JBColor(0xC7222D, 0xFF5261)
        CheckStatus.Solved -> JBColor(0x368746, 0x499C54)
        else -> foreground
      }

      text = when (status) {
        CheckStatus.Failed -> "Incorrect"
        CheckStatus.Solved -> when (task) {
          is IdeTask, is TheoryTask -> "Done"
          else -> "Correct"
        }
        else -> ""
      }
      border = JBUI.Borders.empty(8, 16, 0, 0)
    }
  }

  private class TimeLabel(time: Long) : JBLabel() {
    init {
      border = JBUI.Borders.empty(8, 16, 0, 0)
      foreground = UIUtil.getLabelDisabledForeground()
      val alarm = Alarm()
      object : Runnable {
        override fun run() {
          text = DateFormatUtil.formatPrettyDateTime(time)
          alarm.addRequest(this, DateFormatUtil.MINUTE)
        }
      }.run()
    }
  }
}