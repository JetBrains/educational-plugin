@file:JvmName("EduUtilsKt")

package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener

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
}