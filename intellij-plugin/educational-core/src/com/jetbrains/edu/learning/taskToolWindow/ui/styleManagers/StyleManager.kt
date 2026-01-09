package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.intellij.openapi.editor.colors.EditorColorsManager
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
    return if (!JBColor.isBright()) {
      if (StyleResourcesManager.isHighContrast()) {
        Color(TaskToolWindowBundle.value("high.contrast.body.color"))
      }
      else Color((TaskToolWindowBundle.value("darcula.body.color")))
    }
    else {
      JBColor.foreground().asCssColor()
    }
  }

  private fun codeBackground(): Color {
    // The code is highlighted using the current editor color scheem. So the background should be also taken from the current editor scheme.
    val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
    return editorColorsScheme.defaultBackground.asCssColor()
  }

  fun typographyAndColorStylesheet(): String {
    return CssBuilder().apply {
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
        padding = Padding(4.px)
        // For unknown reason Swing panel fails to start when
        // it is kotlinx.css.StyledElement.borderRadius
        "border-radius" to 5.px
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
    }.toString()
  }


  fun tablesStylesheet(): String {
    val themeDependentBorderColor = when {
      JBColor.isBright() -> Color(TaskToolWindowBundle.value("light.table.border.color"))
      StyleResourcesManager.isHighContrast() -> Color(TaskToolWindowBundle.value("high.contrast.table.border.color"))
      else -> Color(TaskToolWindowBundle.value("dracula.table.border.color"))
    }

    val cellsPadding = Padding(0.375.em, 0.8125.em)

    val tableCss = if (JavaUILibrary.isJCEF())
      CssBuilder().apply {
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
      CssBuilder().apply {
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

  fun hintsStylesheet(): String {
    val hintsCss = CssBuilder().apply {
      ".hint_content > p.first-paragraph, .hint_text > p.first-paragraph" {
        marginTop = 0.px
      }
    }

    return hintsCss.toString()
  }

  companion object {
    const val FONT_SIZE_PROPERTY: String = "edu.task.description.font.factor"

    fun resources(content: String = ""): Map<String, String> = StyleResourcesManager.getResources(content)
  }
}