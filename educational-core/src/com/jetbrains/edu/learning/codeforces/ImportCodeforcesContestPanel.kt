package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestIdFromLink
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_HELP_TEXT
import org.apache.commons.lang.math.NumberUtils.isDigits
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ImportCodeforcesContestPanel {
  val panel: JPanel
  val contestURLTextField: JTextField = JTextField()
  private val textLanguageComboBox = ComboBox<TaskTextLanguage>()
  private val helpLabel = JLabel(CODEFORCES_HELP_TEXT)
  private val panelSize = Dimension(500, 100)

  fun getContestId(): Int = CodeforcesContestConnector.getContestId(contestURLTextField.text)
  fun getContestTextLanguage(): TaskTextLanguage = textLanguageComboBox.selectedItem as TaskTextLanguage

  val preferredFocusedComponent: JComponent?
    get() = contestURLTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()

    initLanguageComboBox()

    panel = panel {
      row("Contest URL:") {
        contestURLTextField(comment = helpLabel.text)
      }
      row("Language:") {
        textLanguageComboBox()
      }
    }

    setPanelSize(panelSize)
  }

  private fun initLanguageComboBox() {
    TaskTextLanguage.values().forEach {
      textLanguageComboBox.addItem(it)
    }

    val preferableTextLanguage = CodeforcesSettings.getInstance().codeforcesPreferableTextLanguage
    if (preferableTextLanguage != null && preferableTextLanguage in TaskTextLanguage.values().map { it.name }) {
      textLanguageComboBox.selectedItem = TaskTextLanguage.valueOf(preferableTextLanguage)
    }
  }

  fun isValidCodeforcesLink(): Boolean {
    val link = contestURLTextField.text
    return link.isNotBlank() && (isDigits(link) || (link.contains(CODEFORCES) && getContestIdFromLink(link) != -1))
  }

  private fun setPanelSize(dimension: Dimension, isMinimumSizeEqualsPreferred: Boolean = true) {
    panel.preferredSize = JBUI.size(dimension)
    if (isMinimumSizeEqualsPreferred) {
      panel.minimumSize = panel.preferredSize
    }
  }
}