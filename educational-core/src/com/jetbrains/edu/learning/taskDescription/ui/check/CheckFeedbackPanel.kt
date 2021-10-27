package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.codeforces.actions.CodeforcesCopyAndSubmitAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.createActionLink
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckMessagePanel.Companion.FOCUS_BORDER_WIDTH
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.BorderLayout
import java.util.*
import javax.swing.JPanel

class CheckFeedbackPanel(task: Task, checkResult: CheckResult, alarm: Alarm) : JPanel(BorderLayout()) {
  init {
    if (checkResult.status != CheckStatus.Unchecked) {
      add(ResultLabel(task, checkResult), BorderLayout.WEST)
      if (checkResult.status == CheckStatus.SubmissionFailed) {
        add(createActionLink(EduCoreBundle.message("codeforces.copy.and.submit"), CodeforcesCopyAndSubmitAction.ACTION_ID, top = 0))
      }
    }
    val checkTime = task.feedback?.time
    if (checkTime != null) {
      add(TimeLabel(checkTime, alarm), BorderLayout.CENTER)
    }
    border = JBUI.Borders.emptyTop(16)
  }

  override fun isVisible(): Boolean = componentCount > 0

  private class ResultLabel(task: Task, checkResult: CheckResult) : JBLabel() {
    init {
      val status = checkResult.status

      icon = when (status) {
        CheckStatus.Failed -> AllIcons.General.BalloonError
        CheckStatus.Solved -> EducationalCoreIcons.ResultCorrect
        CheckStatus.RemoteSubmitted -> AllIcons.General.BalloonInformation
        CheckStatus.SubmissionFailed -> AllIcons.General.BalloonError
        else -> null
      }
      foreground = when (status) {
        CheckStatus.Failed -> EduColors.wrongLabelForeground
        CheckStatus.Solved -> EduColors.correctLabelForeground
        CheckStatus.SubmissionFailed -> EduColors.wrongLabelForeground
        else -> foreground
      }

      text = when (status) {
        CheckStatus.Failed -> "Incorrect"
        CheckStatus.Solved -> when (task) {
          is IdeTask, is TheoryTask -> "Done"
          else -> "Correct"
        }
        CheckStatus.SubmissionFailed, CheckStatus.RemoteSubmitted -> {
          setCopyable(true)
          checkResult.message
        }
        else -> ""
      }

      iconTextGap = JBUI.scale(4)
      border = JBUI.Borders.empty(0, FOCUS_BORDER_WIDTH, 0, 16 - FOCUS_BORDER_WIDTH)
    }
  }

  private class TimeLabel(private val time: Date, private val alarm: Alarm) : JBLabel() {
    init {
      border = JBUI.Borders.empty(0, FOCUS_BORDER_WIDTH, 0, 0)
      foreground = UIUtil.getLabelDisabledForeground()

      val timeUpdater = object : Runnable {
        override fun run() {
          text = DateFormatUtil.formatPrettyDateTime(time)
          alarm.addRequest(this, DateFormatUtil.MINUTE)
        }
      }
      timeUpdater.run()
    }
  }
}