package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskToolWindow.ui.createTextPane
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.Jsoup
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import kotlin.math.min
import kotlin.math.roundToInt

class CheckMessagePanel private constructor() : JPanel() {

  private val messagePane: JTextPane = createTextPane().apply {
    border = JBUI.Borders.emptyTop(16)
  }

  private val messageIconLabel: JBLabel = JBLabel()

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.emptyLeft(FOCUS_BORDER_WIDTH)

    val messagePanel = JPanel(BorderLayout())
    messagePanel.add(messagePane, BorderLayout.CENTER)
    messagePanel.add(messageIconLabel, BorderLayout.WEST)
    add(messagePanel)
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
    val expected = createLabeledComponent(diff.expected, EduCoreBundle.message("compare.outputs.expected"))
    val actual = createLabeledComponent(diff.actual, EduCoreBundle.message("compare.outputs.actual"))
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

  private fun adjustView(checkResult: CheckResult) {
    if (!checkResult.severity.isInfo() || (checkResult.status == CheckStatus.Unchecked && checkResult.message.isNotBlank())) {
      val icon = when (checkResult.status) {
        CheckStatus.Unchecked -> if (checkResult.severity.isWaring()) AllIcons.General.BalloonWarning else AllIcons.General.BalloonInformation
        CheckStatus.Failed -> EducationalCoreIcons.CheckPanel.ResultIncorrect
        else -> null
      }
      messagePane.foreground = when (checkResult.status) {
        CheckStatus.Unchecked -> if (checkResult.severity.isWaring()) EduColors.warningTextForeground else messagePane.foreground
        CheckStatus.Failed -> EduColors.errorTextForeground
        else -> messagePane.foreground
      }
      messageIconLabel.icon = icon
      messageIconLabel.border = JBUI.Borders.empty(16, 0, 0, 4)
    }
  }

  companion object {
    val FOCUS_BORDER_WIDTH = if (SystemInfo.isMac) 3 else if (SystemInfo.isWindows) 0 else 2

    const val MAX_MESSAGE_LENGTH = 300
    const val MAX_LINES_NUMBER = 3

    fun create(checkResult: CheckResult): CheckMessagePanel {
      val messagePanel = CheckMessagePanel()
      messagePanel.setMessage(checkResult.message)
      messagePanel.setHyperlinkListener(checkResult.hyperlinkListener)
      messagePanel.adjustView(checkResult)
      val diff = checkResult.diff
      if (diff != null) {
        messagePanel.setDiff(diff)
      }
      return messagePanel
    }

    private val CheckResult.hyperlinkListener: HyperlinkListener
      get() {
        if (hyperlinkAction == null) {
          return EduBrowserHyperlinkListener.INSTANCE
        }
        return object : HyperlinkAdapter() {
          override fun hyperlinkActivated(e: HyperlinkEvent) {
            hyperlinkAction?.invoke()
          }
        }
      }

    @VisibleForTesting
    fun prepareHtmlText(text: String): String {
      // If text represents HTML text, then next value will be true
      // Because existing HTML text with tags won't be equal to plain text without tags
      val isHtml = Jsoup.parse(text).text() != text
      return if (isHtml) {
        text
      }
      else {
        // But if it's not HTML text, then we should wrap it with general HTML tags
        HtmlBuilder()
          .append(HtmlChunk.head())
          .append(HtmlChunk.body().addText(text))
          .wrapWith(HtmlChunk.html())
          .toString()
      }
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
    lines = convertedStr.lines().toTypedArray()
    return lines
  }
}

private class MultiLineLabel(text: String?) : com.intellij.openapi.ui.ex.MultiLineLabel(text) {
  override fun updateUI() {
    setUI(MultiLineLabelUI())
  }
}
