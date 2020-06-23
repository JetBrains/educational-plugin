package com.jetbrains.edu.learning.twitter.ui

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JTextArea

class DefaultTwitterDialogPanel(
  configurator: TwitterPluginConfigurator,
  solvedTask: Task
) : TwitterDialogPanel(VerticalFlowLayout(0, 0)) {

  private val twitterTextField: JTextArea = JTextArea(4, 0)

  init {
    border = JBUI.Borders.empty()
    twitterTextField.lineWrap = true
    twitterTextField.text = configurator.getDefaultMessage(solvedTask)
    // JTextArea doesn't support scrolling itself
    val scrollPane = JBScrollPane(twitterTextField)
    add(scrollPane)

    val imageUrl = configurator.javaClass.getResource(configurator.getImageResourcePath(solvedTask))
    if (imageUrl != null) {
      val iconLabel = JLabel(ImageIcon(imageUrl))
      iconLabel.border = JBUI.Borders.empty(10, 0, 0, 0)
      add(iconLabel)
    }
  }

  override val message: String get() = twitterTextField.text

  override fun doValidate(): ValidationInfo? {
    val extraCharacters = twitterTextField.text.length - 280
    return if (extraCharacters > 0) {
      ValidationInfo(EduCoreBundle.message("twitter.validation.maximum.length", extraCharacters), twitterTextField)
    }
    else {
      super.doValidate()
    }
  }
}
