package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.NlsContexts.Label
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import org.jetbrains.annotations.NonNls
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

/**
 * Out of the box allows to use hyperlinks in the middle of any text  or wrap the whole text with a link.
 * One can pass custom link handler as lambda, if it's not passed [EduBrowserHyperlinkListener] is used.
 *
 * @param wrapWithLink if true text will be wrapped with `<a href="">text</a>`
 * @param linkHandler action to perform on link click
 */
class EduHyperlinkLabel(
  @Suppress("UnstableApiUsage") @Label text: String,
  wrapWithLink: Boolean = false,
  private val linkHandler: ((HyperlinkEvent) -> Unit)? = null
) : JBLabel() {
  init {
    @NonNls
    val formattedText = """<a href="">$text</a>"""
    setText(UIUtil.toHtml(if (wrapWithLink) formattedText else text))
    setCopyable(true) // this enables hyperlinks support
  }

  override fun createHyperlinkListener(): HyperlinkListener {
    return if (linkHandler == null) {
      return EduBrowserHyperlinkListener.INSTANCE
    }
    else {
      object : HyperlinkAdapter() {
        override fun hyperlinkActivated(e: HyperlinkEvent) {
          linkHandler.invoke(e)
        }
      }
    }
  }
}