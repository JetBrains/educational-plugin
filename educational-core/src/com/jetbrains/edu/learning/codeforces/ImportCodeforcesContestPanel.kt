package com.jetbrains.edu.learning.codeforces

import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestIdFromLink
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_HELP_TEXT
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.apache.commons.lang.math.NumberUtils.isDigits
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ImportCodeforcesContestPanel {
  val panel: JPanel
  val contestURLTextField: JTextField = JTextField()
  private val helpLabel: JLabel = JLabel(CODEFORCES_HELP_TEXT)
  private val panelSize: Dimension = Dimension(500, 60)

  fun getContestId(): Int = CodeforcesContestConnector.getContestId(contestURLTextField.text)

  val preferredFocusedComponent: JComponent?
    get() = contestURLTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()

    panel = panel {
      row("${EduCoreBundle.message("label.codeforces.contest.url")}:") {
        contestURLTextField(comment = helpLabel.text)
      }
    }.apply {
      preferredSize = JBUI.size(panelSize)
      minimumSize = preferredSize
    }
  }

  fun isValidCodeforcesLink(): Boolean {
    val link = contestURLTextField.text
    return link.isNotBlank() && (isDigits(link) || (link.contains(CODEFORCES) && getContestIdFromLink(link) != -1))
  }
}