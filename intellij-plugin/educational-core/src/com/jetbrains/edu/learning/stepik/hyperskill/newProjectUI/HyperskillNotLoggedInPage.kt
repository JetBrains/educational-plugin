package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel

class HyperskillNotLoggedInPanel : Wrapper() {
  private val jbaAcademyHyperskillLogoIcon = IconLoader.getIcon(
    "selectCourseDialog/academyHyperskill.svg",
    HyperskillNotLoggedInPanel::class.java.classLoader
  )
  val topPanelBackground = JBColor.namedColor("Grey2")

  init {
    val leftPanel = createLeftPanel()
    val banner = createBannerPanel()

    val contentPanel = panel {
      row {
        cell(leftPanel).align(com.intellij.ui.dsl.builder.AlignY.TOP)
        cell(banner).align(AlignX.FILL)
      }
    }.apply {
      background = topPanelBackground
      minimumSize = JBDimension(560, 250)
      maximumSize = JBDimension(this.maximumSize.width, 250)
    }

    setContent(Wrapper(contentPanel))
  }

  private fun createBannerPanel(): DialogPanel {
    val academyOnHyperskillBanner = IconUtil.scale(IconLoader.getIcon(
      "selectCourseDialog/hyperskill-freemium-cover-light@2x.png",
      HyperskillNotLoggedInPanel::class.java.classLoader
    ), this, 0.5f)

    return panel {
      row {
        cell(JLabel(academyOnHyperskillBanner))
      }
    }.apply {
      preferredSize = JBDimension(310, 250)
    }
  }

  private fun createLeftPanel(): DialogPanel {
    return panel {
      row {
        val bannerHeight = 40
        val scale = JBUI.scale(bannerHeight).toFloat() / jbaAcademyHyperskillLogoIcon.iconHeight
        cell(object : JLabel(IconUtil.scale(jbaAcademyHyperskillLogoIcon, null, scale)) {
          override fun setBackground(bg: Color?) {
            // Deny changing background by the tool window framework
            super.setBackground(topPanelBackground)
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
        text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.label.text")).applyToComponent {
          font = JBFont.regular().biggerOn(7f).deriveFont(Font.PLAIN)
        }
      }
      row {
        text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.description"), maxLineLength = 55)
      }
    }.apply {
      background = topPanelBackground
      isOpaque = true
    }
  }
}