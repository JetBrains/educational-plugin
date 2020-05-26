package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
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
import java.util.*
import javax.swing.JPanel

class FeedbackPanel(task: Task, parent: Disposable) : JPanel(BorderLayout()), Disposable {
  init {
    Disposer.register(parent, this)
    add(ResultLabel(task), BorderLayout.CENTER)
    val checkTime = task.feedback?.time
    if (checkTime != null) {
      add(TimeLabel(checkTime, this), BorderLayout.EAST)
    }
  }

  override fun dispose() {
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

  private class TimeLabel(private val time: Date, disposable: Disposable) : JBLabel(), Runnable {
    private val alarm = Alarm(disposable)

    init {
      border = JBUI.Borders.empty(8, 16, 0, 0)
      foreground = UIUtil.getLabelDisabledForeground()
      run()
    }

    override fun run() {
      text = DateFormatUtil.formatPrettyDateTime(time)
      alarm.addRequest(this, DateFormatUtil.MINUTE)
    }
  }
}