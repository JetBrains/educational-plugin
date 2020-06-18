package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.TwitterUtils.TwitterDialogPanel
import org.jetbrains.annotations.NonNls
import java.io.InputStream
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JTextArea

class KtTwitterDialogPanel(private val solvedTask: Task) : TwitterDialogPanel(VerticalFlowLayout(0, 0)) {
  private val twitterTextField: JTextArea = JTextArea(4, 0)

  init {
    border = JBUI.Borders.empty()
    twitterTextField.lineWrap = true
    val messageForTask = getMessageForTask(solvedTask)
    twitterTextField.text = messageForTask
    // JTextArea doesn't support scrolling itself
    val scrollPane = JBScrollPane(twitterTextField)
    add(scrollPane)

    val imageUrl = javaClass.getResource(getImagePath(solvedTask))
    if (imageUrl != null) {
      val iconLabel = JLabel(ImageIcon(imageUrl))
      iconLabel.border = JBUI.Borders.empty(10, 0, 0, 0)
      add(iconLabel)
    }
  }

  override fun getMessage(): String = twitterTextField.text
  override fun getMediaSource(): InputStream? = javaClass.getResourceAsStream(getImagePath(solvedTask))
  override fun getMediaExtension(): String? = "gif"

  override fun doValidate(): ValidationInfo? {
    val extraCharacters = twitterTextField.text.length - 280
    return if (extraCharacters > 0) {
      ValidationInfo("Maximum length is 280 characters. Extra characters: ${extraCharacters}", twitterTextField)
    }
    else {
      super.doValidate()
    }
  }

  companion object {
    @NonNls
    private val COMPLETE_KOTLIN_KOANS_LEVEL = "Hey, I just completed level %d of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans"

    private fun getMessageForTask(task: Task): String {
      val solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task)
      return String.format(COMPLETE_KOTLIN_KOANS_LEVEL, solvedTaskNumber / 8)
    }

    private fun getImagePath(task: Task): String {
      val solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task)
      val level = solvedTaskNumber / 8
      return level.toString() + "level.gif"
    }
  }
}
