package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.Animator
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JPanel

class CheckTimer(private val endDateTime: Date, onFinish: () -> Unit = {}) : JPanel(BorderLayout()), Disposable {
  private val animator: Animator

  private val epochMillisLeft: Long
    get() {
      if (endDateTime <= Date()) {
        return 0
      }
      return endDateTime.toInstant().minusMillis(Date().time).toEpochMilli()
    }

  init {
    val clock = JBLabel(EducationalCoreIcons.TaskToolWindow.Clock).apply {
      border = JBUI.Borders.empty(8, 1, 0, 1)
      font = JBFont.regular()
    }
    add(clock, BorderLayout.WEST)

    val timeLabel = JBLabel(epochMillisLeft.format()).apply {
      border = JBUI.Borders.empty(8, 1, 0, 6)
      font = JBFont.regular()
    }
    add(timeLabel, BorderLayout.CENTER)

    animator = object : Animator("CheckTimer", 120, 60 * 1000, true, false) {
      override fun paintNow(frame: Int, totalFrames: Int, cycle: Int) {
        val millisLeft = epochMillisLeft
        if (millisLeft <= 0L) {
          onFinish()
          suspend()
          return
        }

        timeLabel.text = millisLeft.format()
        timeLabel.revalidate()
        timeLabel.repaint()
      }
    }
    Disposer.register(this, animator.toDisposable())

    animator.resume()
  }

  private fun Long.format(): String {
    val timeLeft = Date(this)
    val timeString = dateFormatter.format(timeLeft)
    // "Time left: 04m 03s"
    return "${EduCoreBundle.message("time.left")}: $timeString"
  }

  override fun dispose() {}

  companion object {
    private val dateFormatter = SimpleDateFormat("mm'm' ss's'")
  }
}