package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJavaFxOrJCEF
import com.jetbrains.edu.learning.taskDescription.ui.EduToolsResourcesRequestHandler.Companion.eduResourceUrl
import kotlinx.css.Color

class StyleManager {
  private val typographyManager = TypographyManager()

  val bodyFontSize = typographyManager.bodyFontSize
  val codeFontSize = typographyManager.codeFontSize
  val bodyLineHeight = typographyManager.bodyLineHeight
  val codeLineHeight = typographyManager.codeLineHeight
  val bodyFont = typographyManager.bodyFont
  val codeFont = typographyManager.codeFont

  val bodyColor = bodyColor()
  val linkColor = JBUI.CurrentTheme.Link.linkColor().asCssColor()
  val bodyBackground = JBColor.background().asCssColor()
  val codeBackground = if (isJavaFxOrJCEF()) codeBackground()
  else ColorUtil.dimmer(UIUtil.getPanelBackground()).asCssColor()

  val textStyleHeader = "style=font-size:${bodyFontSize}"

  val scrollBarStylesheets = getScrollBarStylesheetsUrls()
  val baseStylesheet = eduResourceUrl("/style/browser.css")

  fun resources(content: String) = StyleResourcesManager(content).resources

  private fun getScrollBarStylesheetsUrls(): List<String> {
    return listOf(eduResourceUrl("/style/scrollbars/base.css"),
                  if (SystemInfo.isWindows) eduResourceUrl("/style/scrollbars/winShape.css")
                      else eduResourceUrl("/style/scrollbars/macLinuxShape.css"),
                  eduResourceUrl("/style/scrollbars/${resourceFileName()}.css"))
  }

  private fun java.awt.Color.asCssColor(): Color = Color("#${ColorUtil.toHex(this)}")

  private fun bodyColor(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.message("darcula.body.color"))) else JBColor.foreground().asCssColor()
  }

  private fun codeBackground(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.message("darcula.code.background")))
    else Color(TaskDescriptionBundle.message("code.background"))
  }

  companion object {
    internal val LOG = Logger.getInstance(this::class.java)
    const val FONT_FACTOR_PROPERTY = "edu.task.description.font.factor"
  }
}

internal fun resourceFileName(): String {
  return when {
    isHighContrast() -> "highcontrast"
    UIUtil.isUnderDarcula() -> "darcula"
    else -> "light"
  }
}

internal fun isHighContrast(): Boolean {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo ?: return false
  return lookAndFeel.theme.id == "JetBrainsHighContrastTheme"
}