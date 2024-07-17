package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.loadIcon
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class HyperskillSelectTrackPanel : JPanel(BorderLayout()) {
  private val icon = loadIcon("selectCourseDialog/hyperskill/select-track.png", this::class.java.classLoader)

  init {
    val mainPanel = panel {
      row {
        val bannerPanel = Wrapper(JLabel(icon))
        cell(bannerPanel).align(Align.FILL)
      }
      row {
        text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.choose.your.track")).applyToComponent {
          font = JBFont.h1().asBold()
        }.align(Align.CENTER)
      }.customize(UnscaledGapsY(40))
      row {
        val text = HtmlBuilder()
          .appendRaw(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.choose.your.track.description"))
          .wrapWith(HtmlChunk.div("text-align: center;"))
          .toString()
        comment(text, maxLineLength = 30).align(Align.CENTER)
      }
      row {
        button(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.select.track")) {
          BrowserUtil.open("https://hyperskill.org/tracks")
          val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, this@HyperskillSelectTrackPanel)
          dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
        }.applyToComponent {
          this.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, true)
          requestFocus()
        }.align(Align.CENTER)
      }.topGap(TopGap.SMALL)
    }


    add(mainPanel, BorderLayout.CENTER)
  }
}