package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.taskToolWindow.ui.EduBrowserHyperlinkListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.text.html.HTMLEditorKit

class AdditionalInformationPanel private constructor(
  private val project: Project
) : JBPanel<AdditionalInformationPanel>(BorderLayout()) {

  private lateinit var informationText: JEditorPane

  init {
    border = JBUI.Borders.empty(8, 0)

    createInformationText()
  }

  fun createInformationText() {
    informationText = JEditorPane()

    with(informationText) {
      isEditable = false
      contentType = "text/html"
      isFocusable = false
      border = null
      background = null
      margin = JBUI.emptyInsets()

      setupInfoFont()

      addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)
    }

    add(informationText, BorderLayout.CENTER)
  }

  fun updateSize() {
    remove(informationText)
    createInformationText()
    updateText()
  }

  private fun JEditorPane.setupInfoFont() {
    val labelFont = UIManager.getFont("Label.font") ?: return
    val leftMargin = JBUI.scale(2)

    editorKit = HTMLEditorKit().apply {
      styleSheet.addRule(
        """
        body {
          font-family: ${labelFont.family};
          font-size: ${labelFont.size}pt;
          margin-left: ${leftMargin}px;
        }
        """
      )
    }
  }

  private fun updateText() {
    val texts = CheckButtonAdditionalInformationManager.instance(project).additionalInformation.value.allTexts
    updateText(texts)
  }

  private fun updateText(texts: String?) {
    isVisible = texts != null
    if (texts == null) return
    informationText.text = "<html>$texts</html>"
  }

  companion object {
    fun create(project: Project): AdditionalInformationPanel {
      val panel = AdditionalInformationPanel(project)

      panel.updateText()

      currentThreadCoroutineScope().launch {
        CheckButtonAdditionalInformationManager.instance(project).additionalInformation.collectLatest {
          panel.updateText(it.allTexts)
        }
      }

      return panel
    }
  }
}