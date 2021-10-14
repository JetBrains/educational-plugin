package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.Jsoup
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.event.HyperlinkListener
import kotlin.math.min
import kotlin.math.roundToInt

class CheckMessagePanel private constructor() : JPanel() {

  private val messagePane: JTextPane = createTextPane().apply {
    border = JBUI.Borders.emptyTop(16)
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.emptyLeft(FOCUS_BORDER_WIDTH)
    add(messagePane)
  }

  var messageShortened: Boolean = false

  override fun isVisible(): Boolean =
    componentCount > 1 || messagePane.document.getText(0, messagePane.document.length).isNotEmpty()

  private fun setMessage(message: String) {
    val lines = message.lines()
    val displayMessage = if (message.length > MAX_MESSAGE_LENGTH || lines.size > MAX_LINES_NUMBER) {
      messageShortened = true
      // we could potentially cut message in the middle of html tag
      val messageLinesCut = lines.subList(0, min(lines.size, MAX_LINES_NUMBER)).joinToString("\n")
      messageLinesCut.substring(0, min(messageLinesCut.length, MAX_MESSAGE_LENGTH)) + "..."
    }
    else {
      messageShortened = false
      message
    }
    messagePane.text = prepareHtmlText(displayMessage)
  }

  private fun setHyperlinkListener(listener: HyperlinkListener) {
    messagePane.addHyperlinkListener(listener)
  }

  private fun setDiff(diff: CheckResultDiff) {
    val expected = createLabeledComponent(diff.expected, "Expected")
    val actual = createLabeledComponent(diff.actual, "Actual")
    UIUtil.mergeComponentsWithAnchor(expected, actual)

    add(expected)
    add(actual)
  }

  private fun createLabeledComponent(resultText: String, labelText: String): LabeledComponent<JComponent> {
    val lines = resultText.lines()
    val displayMessage = if (lines.size > MAX_LINES_NUMBER)
      lines.subList(0, MAX_LINES_NUMBER).joinToString("\n") + "..."
    else resultText

    val textPane = MultiLineLabel(displayMessage).apply {
      // `JBUI.Fonts.create` implementation scales font size.
      // Also, at the same time `font.size` returns scaled size.
      // So we have to pass non scaled font size to create font with correct size
      font = JBUI.Fonts.create(Font.MONOSPACED, (font.size / JBUIScale.scale(1f)).roundToInt())
      verticalAlignment = JLabel.TOP
    }
    val labeledComponent = LabeledComponent.create<JComponent>(textPane, labelText, BorderLayout.WEST)
    labeledComponent.label.foreground = UIUtil.getLabelDisabledForeground()
    labeledComponent.label.verticalAlignment = JLabel.TOP
    labeledComponent.border = JBUI.Borders.emptyTop(8)
    return labeledComponent
  }

  companion object {
    val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2

    const val MAX_MESSAGE_LENGTH = 300
    const val MAX_LINES_NUMBER = 3

    @JvmStatic
    fun create(checkResult: CheckResult): CheckMessagePanel {
      val messagePanel = CheckMessagePanel()
      if (checkResult.status == CheckStatus.RemoteSubmitted || checkResult.status == CheckStatus.RemoteFailed) return messagePanel
      messagePanel.setMessage(checkResult.message)
      messagePanel.setHyperlinkListener(checkResult.hyperlinkListener ?: EduBrowserHyperlinkListener.INSTANCE)
      if (checkResult.diff != null) {
        messagePanel.setDiff(checkResult.diff)
      }
      return messagePanel
    }

    @VisibleForTesting
    fun prepareHtmlText(text: String): String = Jsoup.parse(text).text()
  }
}

private class MultiLineLabelUI : com.intellij.openapi.ui.MultiLineLabelUI() {

  private var text: String? = null
  private var lines: Array<String> = ArrayUtil.EMPTY_STRING_ARRAY

  // Almost identical with original implementation
  // except it doesn't trim lines
  override fun splitStringByLines(str: String?): Array<String> {
    if (str == null) return ArrayUtil.EMPTY_STRING_ARRAY
    if (str == text) return lines
    val convertedStr = convertTabs(str, 2)
    text = convertedStr
    lines = convertedStr.lines().toTypedArray()
    return lines
  }
}

private class MultiLineLabel(text: String?) : com.intellij.openapi.ui.ex.MultiLineLabel(text) {
  override fun updateUI() {
    setUI(MultiLineLabelUI())
  }
}
