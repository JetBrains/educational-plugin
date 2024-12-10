package com.jetbrains.edu.aiDebugging.core.ui

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.PositionTracker
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.ui.EduColors

class AIBreakpointHint(text: String, editor: Editor, offset: Int) {

  private var balloon: Balloon? = null

  init {
    runInEdt {
      balloon = createBalloon(text)

      val tracker = object : PositionTracker<Balloon>(editor.contentComponent) {
        override fun recalculateLocation(balloon: Balloon): RelativePoint {
          val visualPosition = editor.offsetToVisualPosition(offset)
          val point = editor.visualPositionToXY(visualPosition)
          return RelativePoint(editor.contentComponent, point)
        }
      }

      balloon?.show(tracker, Balloon.Position.above)
    }
  }

  private fun createBalloon(text: String): Balloon =
    JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        getInternalTemplateText("text-balloon.html", mapOf("text" to text)),
        AIDebuggingIcons.AIHint,
        EduColors.aiGetHintInlineBannersBackgroundColor,
        null
      )
      .setBorderColor(EduColors.aiGetHintInlineBannersBorderColor)
      .setRequestFocus(false)
      .setCloseButtonEnabled(true)
      .setHideOnClickOutside(false)
      .createBalloon()

  fun close() {
    balloon?.hide()
  }
}
