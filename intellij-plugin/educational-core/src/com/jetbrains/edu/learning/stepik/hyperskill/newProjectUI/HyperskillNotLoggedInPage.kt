package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel

class HyperskillNotLoggedInPanel : Wrapper() {
  val academyOnHyperskillBanner = IconLoader.getIcon(
    "selectCourseDialog/banner.svg",
    HyperskillNotLoggedInPanel::class.java.classLoader
  )
  val jbaAcademyHyperskillLogoIcon = IconLoader.getIcon(
    "selectCourseDialog/academyHyperskill.svg",
    HyperskillNotLoggedInPanel::class.java.classLoader
  )
  val background = Gray.x13

  init {
    val leftColumn = "left"
    val rightColumn = "right"

    val topPanel = panel {
      row {

        val bannerHeight = 40
        val scale = JBUI.scale(bannerHeight).toFloat() / jbaAcademyHyperskillLogoIcon.iconHeight
        cell(object : JLabel(IconUtil.scale(jbaAcademyHyperskillLogoIcon, null, scale)) {
          override fun setBackground(bg: Color?) {
            // Deny changing background by the tool window framework
            super.setBackground(background)
          }
        })
          .align(AlignX.LEFT)
          .applyToComponent {
            minimumSize = JBDimension(120, bannerHeight)
            preferredSize = JBDimension(120, bannerHeight)
            isOpaque = true
          }

      }
      row {
        text("JetBrains Academy on Hyperskill").applyToComponent {
          font = JBFont.regular().biggerOn(7f).deriveFont(Font.PLAIN)
        }
      }
      row {
        text(
          "Together with Hyperskill, JetBrains Academy provides a project-based approach to learning computer science available right in " +
          "JetBrains IDEs", maxLineLength = 55)
      }
    }.apply {
      background = background
      isOpaque = true
    }

    val banner = panel {
      row {

        val width = 180
        val height = 280
        val scale = JBUI.scale(height).toFloat() / academyOnHyperskillBanner.iconHeight
        cell(object : JLabel(IconUtil.scale(academyOnHyperskillBanner, null, scale)) {
          override fun setBackground(bg: Color?) {
            // Deny changing background by the tool window framework
            super.setBackground(background)
          }
        })
          .align(AlignX.LEFT)
          .applyToComponent {
            minimumSize = JBDimension(82, height)
            preferredSize = JBDimension(width, height)
            isOpaque = true
          }
      }
    }.apply {
      background = background
      isOpaque = true
    }

    val contentPanel = panel {
      row {
        cell(topPanel)
        cell(banner).align(AlignX.FILL)
      }
    }.apply {
      background = JBColor.namedColor("SelectCourse.", JBColor(0xFFFFFF, 0x1E1F22))
    }

    setContent(contentPanel)
  }
}