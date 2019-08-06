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
  private val languageComboBox = ComboBox<LANGUAGE>()
  private val helpLabel = JLabel(CODEFORCES_HELP_TEXT)
  private val panelSize = Dimension(500, 100)

  fun contestId(): Int = CodeforcesContestConnector.getContestId(contestURLTextField.text)
  fun contestLanguage(): String = (languageComboBox.selectedItem as LANGUAGE).locale

  val preferredFocusedComponent: JComponent?
    get() = contestURLTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()

    LANGUAGE.values().forEach {
      languageComboBox.addItem(it)
    }

    panel = panel {
      row("Contest URL:") {
        contestURLTextField(comment = helpLabel.text)
      }
      row("Language:") {
        languageComboBox()
      }
    }

    setPanelSize(panelSize)
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

  @Suppress("unused")
  private enum class LANGUAGE(val locale: String) {
    ENGLISH("en") {
      override fun toString() = "English"
    },
    RUSSIAN("ru") {
      override fun toString() = "Русский"
    }
  }
}