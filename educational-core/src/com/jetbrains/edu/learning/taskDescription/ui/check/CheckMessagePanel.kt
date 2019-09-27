package com.jetbrains.edu.learning.taskDescription.ui.check

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import kotlinx.css.CSSBuilder
import kotlinx.css.body
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class CheckMessagePanel private constructor(): JPanel() {

  private val messagePane: JTextPane = com.jetbrains.edu.learning.taskDescription.ui.createTextPane().apply {
    border = JBUI.Borders.empty()
    addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.emptyLeft(FOCUS_BORDER_WIDTH)
    add(messagePane)
  }

  val isEmpty: Boolean get() = messagePane.document.getText(0, messagePane.document.length).isEmpty() && componentCount == 1

  private fun setMessage(message: String) {
    var displayMessage = if (message.length > MAX_MESSAGE_LENGTH) message.substring(0, MAX_MESSAGE_LENGTH) + "..." else message
    displayMessage = StringUtil.replace(displayMessage, listOf(" ", "\n"), listOf("&nbsp;", "<br>"))
    if (message.contains("expected", true) && message.contains("actual", true)) {
      displayMessage = displayMessage.monospaced()
    }
    messagePane.text = displayMessage
  }

  private fun setDiff(diff: CheckResultDiff) {
    val (expectedText, actualText, _, message) = diff
    setMessage(message)
    val expected = createLabeledComponent(expectedText, "Expected", 16)
    val actual = createLabeledComponent(actualText, "Actual", 8)
    UIUtil.mergeComponentsWithAnchor(expected, actual)

    add(expected)
    add(actual)
  }

  private fun createLabeledComponent(resultText: String, labelText: String, topPadding: Int): LabeledComponent<JComponent> {
    val text = if (resultText.length > MAX_EXPECTED_ACTUAL_LENGTH) resultText.substring(0, MAX_EXPECTED_ACTUAL_LENGTH) + "..." else resultText
    val textPane = MultiLineLabel(text).apply {
      // `JBUI.Fonts.create` implementation scales font size.
      // Also, at the same time `font.size` returns scaled size.
      // So we have to pass non scaled font size to create font with correct size
      font = JBUI.Fonts.create(Font.MONOSPACED, Math.round(font.size / JBUI.scale(1f)))
      verticalAlignment = JLabel.TOP
    }
    val labeledComponent = LabeledComponent.create<JComponent>(textPane, labelText, BorderLayout.WEST)
    labeledComponent.label.foreground = UIUtil.getLabelDisabledForeground()
    labeledComponent.label.verticalAlignment = JLabel.TOP
    labeledComponent.border = JBUI.Borders.emptyTop(topPadding)
    return labeledComponent
  }

  companion object {
    private val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2

    const val MAX_MESSAGE_LENGTH = 400
    const val MAX_EXPECTED_ACTUAL_LENGTH = 150

    @JvmStatic
    fun create(checkResult: CheckResult): CheckMessagePanel {
      val messagePanel = CheckMessagePanel()
      messagePanel.setMessage(checkResult.escapedMessage)
      if (checkResult.diff != null) {
        messagePanel.setDiff(checkResult.diff)
      }
      return messagePanel
    }
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
    lines = StringUtil.splitByLinesDontTrim(convertedStr)
    return lines
  }
}

private class MultiLineLabel(text: String?) : com.intellij.openapi.ui.ex.MultiLineLabel(text) {
  override fun updateUI() {
    setUI(MultiLineLabelUI())
  }
}

private fun String.monospaced(): String {
  val fontCss = CSSBuilder().apply {
    body {
      fontFamily = StyleManager().codeFont
    }
  }.toString()
  return "<html><head><style>${fontCss}</style></head><body>${this}</body></html>"
}
