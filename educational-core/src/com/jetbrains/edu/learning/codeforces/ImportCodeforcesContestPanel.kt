package com.jetbrains.edu.learning.codeforces

import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestIdFromLink
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_HELP_TEXT
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CODEFORCES_TYPE_YAML
import org.apache.commons.lang.math.NumberUtils.isDigits
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ImportCodeforcesContestPanel {
  val panel: JPanel
  val contestURLTextField: JTextField = JTextField()
  private val helpLabel: JLabel = JLabel(CODEFORCES_HELP_TEXT)

  fun getContestId(): Int = CodeforcesContestConnector.getContestId(contestURLTextField.text)

  val preferredFocusedComponent: JComponent
    get() = contestURLTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()

    panel = panel {
      row("${EduCoreBundle.message("label.codeforces.contest.url")}:") {
        cell(contestURLTextField)
          .columns(COLUMNS_LARGE)
          .align(AlignX.FILL)
          .comment(CODEFORCES_HELP_TEXT)
      }
    }
  }

  fun isValidCodeforcesLink(): Boolean {
    val link = contestURLTextField.text
    return link.isNotBlank() && (isDigits(link) || (link.contains(CODEFORCES_TYPE_YAML) && getContestIdFromLink(link) != -1))
  }
}