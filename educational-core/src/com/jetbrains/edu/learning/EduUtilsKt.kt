@file:JvmName("EduUtilsKt")

package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.util.concurrent.Callable

object EduUtilsKt {
  @JvmStatic
  @JvmOverloads
  fun DataContext.showPopup(htmlContent: String, position: Balloon.Position = Balloon.Position.above) {
    val balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        htmlContent,
        null,
        UIUtil.getToolTipActionBackground(),
        EduBrowserHyperlinkListener.INSTANCE)
      .createBalloon()

    val tooltipRelativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this)
    balloon.show(tooltipRelativePoint, position)
  }

  @JvmStatic
  fun convertToHtml(markdownText: String): String {
    // Markdown parser is supposed to work with normalized text from document
    val normalizedText = StringUtil.convertLineSeparators(markdownText)

    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)

    return HtmlGenerator(normalizedText, parsedTree, flavour, false).generateHtml()
  }
}

class NumericInputValidator(val emptyInputMessage: String, val notNumericMessage: String) : InputValidatorEx {
  override fun getErrorText(inputString: String): String? {
    val input = inputString.trim()
    return when {
      input.isEmpty() -> emptyInputMessage
      !isNumeric(input) -> notNumericMessage
      else -> null
    }
  }

  override fun checkInput(inputString: String): Boolean {
    return getErrorText(inputString) == null
  }

  override fun canClose(inputString: String): Boolean = true

  private fun isNumeric(string: String): Boolean {
    return string.all { StringUtil.isDecimalDigit(it) }
  }
}

object Executor {
  fun <T> execCancelable(message: String, callable: Callable<T>): T =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<T, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable(callable)
      },
      message, true, null)
}

