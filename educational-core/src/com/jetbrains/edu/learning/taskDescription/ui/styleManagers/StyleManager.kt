package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import kotlinx.css.*
import kotlinx.css.properties.lh

class StyleManager {
  private val typographyManager = TypographyManager()

  val bodyFontSize = typographyManager.bodyFontSize
  private val codeFontSize = typographyManager.codeFontSize
  val bodyLineHeight = typographyManager.bodyLineHeight
  private val codeLineHeight = typographyManager.codeLineHeight
  val bodyFont = typographyManager.bodyFont
  val codeFont = typographyManager.codeFont

  val bodyColor = bodyColor()
  private val linkColor = JBUI.CurrentTheme.Link.Foreground.ENABLED.asCssColor()
  val bodyBackground = JBColor.background().asCssColor()
  val codeBackground = if (isJCEF()) codeBackground() else ColorUtil.dimmer(UIUtil.getPanelBackground()).asCssColor()

  val textStyleHeader = "style=font-size:${bodyFontSize}pt"

  private fun bodyColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      if (StyleResourcesManager.isHighContrast()) {
        Color(TaskDescriptionBundle.value("high.contrast.body.color"))
      }
      else Color((TaskDescriptionBundle.value("darcula.body.color")))
    }
    else {
      JBColor.foreground().asCssColor()
    }
  }

  private fun codeBackground(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.value("darcula.code.background")))
    else Color(TaskDescriptionBundle.value("code.background"))
  }

  fun typographyAndColorStylesheet(): String {
    return CSSBuilder().apply {
      body {
        fontFamily = bodyFont
        fontSize = if (isJCEF()) bodyFontSize.px else bodyFontSize.pt
        lineHeight = bodyLineHeight.px.lh
        color = bodyColor
        backgroundColor = bodyBackground
      }

      ".code" {
        fontFamily = codeFont
        backgroundColor = codeBackground
        fontSize = if (isJCEF()) codeFontSize.px else codeFontSize.pt
        padding = "4 4 4 4"
        borderRadius = 5.px
      }

      ".code-block" {
        backgroundColor = codeBackground
        fontSize = if (isJCEF()) bodyFontSize.px else bodyFontSize.pt
        lineHeight = codeLineHeight.px.lh
        display = Display.block
      }

      a {
        color = linkColor
      }

      codeforcesCssAdjustment()
    }.toString()
  }

  private fun codeforcesCssAdjustment(): CSSBuilder =
    CSSBuilder().apply {
      ".compact-problemset .problem-statement" {
        fontSize = bodyFontSize.rem.times(1.4)
      }

      // Commented because otherwise all other font-sizes don't work ¯\_(ツ)_/¯
//      ".problem-statement" {
//        fontSize = styleManager.bodyFontSize.rem.times(1.4)
//      }

      ".problem-statement .header .title" {
        fontSize = bodyFontSize.pct.times(150)
      }

      ".problem-statement .section-title" {
        fontSize = bodyFontSize.pct.times(115)
      }

      ".problem-statement .sample-tests" {
        fontSize = bodyFontSize.em.times(0.9)
      }

      ".problem-statement .sample-tests .title" {
        fontSize = bodyFontSize.em.times(1.3)
      }
    }

  fun tablesStylesheet(): String {
    val themeDependentBorderColor = if (UIUtil.isUnderDarcula()) {
      if (StyleResourcesManager.isHighContrast())
        Color(TaskDescriptionBundle.value("high.contrast.table.border.color"))
      else
        Color(TaskDescriptionBundle.value("dracula.table.border.color"))
    } else
      Color(TaskDescriptionBundle.value("light.table.border.color"))

    val cellsPadding = TaskDescriptionBundle.value("table.cell.padding")

    val tableCss =  if (JavaUILibrary.isJCEF())
      CSSBuilder().apply {
        "table" {
          borderCollapse = BorderCollapse.collapse
        }

        "td, th" {
          borderWidth = 1.px
          borderStyle = BorderStyle.solid
          borderColor = themeDependentBorderColor
          padding = cellsPadding
        }
      }
    else // Swing JTextPane does not support border-collapse, so we implement a workaround
      CSSBuilder().apply {
        "table" {
          backgroundColor = themeDependentBorderColor
        }

        "td, th" {
          backgroundColor = bodyBackground
          padding = cellsPadding // in fact, padding it does not support also
        }
      }

    return tableCss.toString()
  }

  companion object {
    const val FONT_SIZE_PROPERTY: String = "edu.task.description.font.factor"

    fun resources(content: String = ""): Map<String, String> = StyleResourcesManager.getResources(content)
  }
}