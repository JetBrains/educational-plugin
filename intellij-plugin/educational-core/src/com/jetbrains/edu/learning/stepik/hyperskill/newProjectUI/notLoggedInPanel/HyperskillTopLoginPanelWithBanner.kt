package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.util.height
import com.intellij.ui.util.width
import com.intellij.util.IconUtil
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.JLabel
import javax.swing.JPanel


private const val PANEL_GAP = 40
private const val LOGO_HEIGHT = 40
private const val ICON_PANEL_TOP_GAP = 20
private const val ICON_PANEL_LEFT_GAP = 29
private const val ICON_PANEL_BOTTOM_GAP = 10
private const val ICON_PANEL_RIGHT_GAP = 0
private const val DESCRIPTION_MAX_LENGTH = 43
private const val TITLE_FONT_FACTOR = 7f

class HyperskillTopLoginPanelWithBanner : Wrapper() {
  private val backgroundColor = JBColor.namedColor(
    "SelectCourse.Hyperskill.HyperskillNotLoggedInPanel.LoginBanner.backgroundColor", 0xF7F8FA, 0x2B2D30
  )

  private val oldUIBackgroundColor = JBColor(0xF2F2F2, 0x3C3F41)
  private val radius = JBUI.scale(12)
  private val icon = loadIcon("selectCourseDialog/hyperskill/hyperskill-freemium-cover.png", this::class.java.classLoader)

  init {
    border = JBUI.Borders.empty(PANEL_GAP)

    setContent(JPanel(BorderLayout()).apply {
      val scaledIcon = IconUtil.scale(icon, this@HyperskillTopLoginPanelWithBanner, 0.5f)
      val bannerPanel = Wrapper(JLabel(scaledIcon))

      add(createLeftPanel(), BorderLayout.CENTER)
      add(bannerPanel, BorderLayout.EAST)
    }.apply {
      isOpaque = true
      background = getBackgroundColor()
    })
  }

  // copy-pasted from `com.intellij.ide.startup.importSettings.chooser.ui.RoundedPanel.RoundedJPanel`
  override fun paintComponent(g: Graphics) {
    val g2d = g as Graphics2D
    val config = GraphicsUtil.setupAAPainting(g)

    val ins = insets

    g2d.clip(RoundRectangle2D.Double(ins.left.toDouble(), ins.top.toDouble(), (width - ins.width).toDouble(), (height - ins.height).toDouble(), radius.toDouble(), radius.toDouble()))
    super.paintComponent(g2d)

    config.restore()
  }

  private fun createLeftPanel(): JPanel {
    val iconPanel = createIconPanel()
    val labelAndButtonPanel = createLabelAndButtonPanel()

    return JPanel(VerticalFlowLayout()).apply {
      add(iconPanel)
      add(labelAndButtonPanel)
    }
  }

  private fun createLabelAndButtonPanel() = panel {
    row {
      // I haven't found anything more suitable than text()
      @Suppress("DialogTitleCapitalization") text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.label.text")).applyToComponent {
        font = JBFont.regular().biggerOn(TITLE_FONT_FACTOR).deriveFont(Font.PLAIN)
      }
    }
    row {
      text(
        EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.description"), maxLineLength = DESCRIPTION_MAX_LENGTH
      )
    }
    row {
      button(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.log.in")) {
        HyperskillConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
      }.applyToComponent {
        this.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, true)
        requestFocus()
      }
    }.topGap(TopGap.SMALL)
  }.apply {
    isOpaque = true
    background = getBackgroundColor()
    border = JBUI.Borders.emptyLeft(30)
  }

  private fun createIconPanel(): JPanel {
    val jbaAcademyHyperskillLogoIcon = loadIcon(
      "selectCourseDialog/hyperskill/academyHyperskill.svg", HyperskillTopLoginPanelWithBanner::class.java.classLoader
    )

    val scale = JBUI.scale(LOGO_HEIGHT).toFloat() / jbaAcademyHyperskillLogoIcon.iconHeight
    val label = object : JLabel(IconUtil.scale(jbaAcademyHyperskillLogoIcon, null, scale)) {
      override fun setBackground(bg: Color?) {
        super.setBackground(getBackgroundColor())
      }
    }

    val iconPanel = JPanel(BorderLayout()).apply {
      add(label, BorderLayout.WEST)
    }

    return Wrapper(iconPanel).apply {
      border = JBUI.Borders.empty(ICON_PANEL_TOP_GAP, ICON_PANEL_LEFT_GAP, ICON_PANEL_BOTTOM_GAP, ICON_PANEL_RIGHT_GAP)
    }
  }

  private fun getBackgroundColor() = if (ExperimentalUI.isNewUI()) backgroundColor else oldUIBackgroundColor
}
