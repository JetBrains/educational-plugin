package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel


private const val ICON_TEXT_GAP = 32
private const val TOP_AND_BOTTOM_GAP = 41
private const val MIN_PANEL_WIDTH = 672
private const val MIN_PANEL_HEIGHT = 223
private const val MAX_TEXT_LENGTH = 24
private const val MIN_CARD_WIDTH = 216
private const val MIN_CARD_HEIGHT = 200
private const val TITLE_FONT_FACTOR = 3f

class HowItWorksPanel : JPanel(BorderLayout()) {

  init {
    isOpaque = false
    add(panel {
      row {
        text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works")).bold().applyToComponent {
          font = JBFont.regular().biggerOn(TITLE_FONT_FACTOR).deriveFont(Font.PLAIN)
        }
      }
      separator()
      row {
        cell(
          HowItWorksCard(
            "selectCourseDialog/hyperskill/Features/select-learning-track.png",
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learning.track.title"),
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learning.track.description")
          )
        )
        cell(
          HowItWorksCard(
            "selectCourseDialog/hyperskill/Features/learn-by-doing.png",
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learn.by.doing.title"),
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learn.by.doing.description")
          )
        )
        cell(
          HowItWorksCard(
            "selectCourseDialog/hyperskill/Features/create-real-world-apps.png",
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.title"),
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.description"),
            EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.comment")
          )
        )
      }.customize(UnscaledGapsY(ICON_TEXT_GAP))
    }.apply {
      isOpaque = false
      minimumSize = JBDimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT)
      border = JBUI.Borders.empty(TOP_AND_BOTTOM_GAP, 0)
    })
  }

  private class HowItWorksCard(
    iconPath: String,
    title: String,
    description: String,
    comment: String = ""
  ) : Wrapper() {

    init {
      val icon = IconUtil.resizeSquared(loadIcon(iconPath, HyperskillNotLoggedInPanel::class.java.classLoader), 48)

      setContent(panel {
        row {
          icon(icon)
        }
        row {
          text(title).bold()
        }
        row {
          text(description, MAX_TEXT_LENGTH)
        }
        row {
          comment(comment, MAX_TEXT_LENGTH)
        }
      }.apply {
        isOpaque = false
        minimumSize = JBDimension(MIN_CARD_WIDTH, MIN_CARD_HEIGHT)
      })
    }
  }
}