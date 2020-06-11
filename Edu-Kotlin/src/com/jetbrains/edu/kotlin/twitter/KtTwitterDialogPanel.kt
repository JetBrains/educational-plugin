package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.TwitterUtils.TwitterDialogPanel
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.io.InputStream
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentListener

class KtTwitterDialogPanel(solvedTask: Task) : TwitterDialogPanel() {
  private val myTwitterTextField: JTextArea
  private val myRemainSymbolsLabel: JLabel
  private var myImageUrl: URL? = null
  private var imageName = ""

  init {
    layout = VerticalFlowLayout()
    myRemainSymbolsLabel = JLabel()
    myTwitterTextField = JTextArea()
    myTwitterTextField.lineWrap = true
    create(solvedTask)
  }


  private fun create(solvedTask: Task) {
    add(JLabel(UIUtil.toHtml(POST_ACHIEVEMENT_HTML)))
    myImageUrl = getMediaSourceForTask(solvedTask)
    addImageLabel()
    val messageForTask = getMessageForTask(solvedTask)
    myTwitterTextField.text = messageForTask
    add(myTwitterTextField)
    myRemainSymbolsLabel.text = (140 - messageForTask.length).toString()
    val jPanel = JPanel(BorderLayout())
    jPanel.add(myRemainSymbolsLabel, BorderLayout.EAST)
    add(jPanel)
  }

  private fun addImageLabel() {
    if (myImageUrl != null) {
      val icon: Icon = ImageIcon(myImageUrl)
      add(JLabel(icon))
    }
  }

  override fun getMessage(): String = myTwitterTextField.text
  override fun getMediaSource(): InputStream? = javaClass.getResourceAsStream(imageName)
  override fun getMediaExtension(): String? = "gif"

  private fun getMediaSourceForTask(task: Task): URL? {
    imageName = "/twitter/kotlin_koans/images/" + getImageName(task)
    return javaClass.getResource(imageName)
  }

  override fun addTextFieldVerifier(documentListener: DocumentListener) {
    myTwitterTextField.document.addDocumentListener(documentListener)
  }

  override fun getRemainSymbolsLabel(): JLabel = myRemainSymbolsLabel

  companion object {
    @NonNls
    private val POST_ACHIEVEMENT_HTML = "<b>Post your achievements to twitter!<b>\n"

    @NonNls
    private val COMPLETE_KOTLIN_KOANS_LEVEL = "Hey, I just completed level %d of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans"
    private fun getMessageForTask(task: Task): String {
      val solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task)
      return String.format(COMPLETE_KOTLIN_KOANS_LEVEL, solvedTaskNumber / 8)
    }

    private fun getImageName(task: Task): String {
      val solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task)
      val level = solvedTaskNumber / 8
      return level.toString() + "level.gif"
    }
  }
}
