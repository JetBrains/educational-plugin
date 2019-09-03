package com.jetbrains.edu.learning.ui.taskDescription.styleManagers

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
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
  val codeBackground = if (EduSettings.getInstance().shouldUseJavaFx()) bodyBackground
  else ColorUtil.dimmer(UIUtil.getPanelBackground()).asCssColor()

  val scrollBarStylesheets = getScrollBarStylesheetsUrls()
  val baseStylesheet = resourceUrl("/style/browser.css")
  val buttonStylesheets = listOfNotNull(baseStylesheet,
                                        resourceUrl(
                                          "/style/javafxButtons/buttonsBase.css"),
                                        resourceUrl(
                                          "/style/javafxButtons/buttonsDarcula.css").takeIf { UIUtil.isUnderDarcula() })

  fun resources(content: String) = StyleResourcesManager(content).resources

  private fun getScrollBarStylesheetsUrls(): List<String> {
    return listOf(resourceUrl("/style/scrollbars/base.css"),
                  if (SystemInfo.isWindows) resourceUrl(
                    "/style/scrollbars/winShape.css")
                  else resourceUrl("/style/scrollbars/macLinuxShape.css"),
                  resourceUrl("/style/scrollbars/${resourceFileName()}.css"))
  }

  private fun java.awt.Color.asCssColor(): Color = Color("#${ColorUtil.toHex(this)}")

  private fun bodyColor(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.message("darcula.body.color"))) else JBColor.foreground().asCssColor()
  }

  companion object {
    internal val LOG = Logger.getInstance(this::class.java)
    const val FONT_FACTOR_PROPERTY = "edu.task.description.font.factor"
  }
}

internal fun resourceUrl(name: String): String {
  val resource = object {}.javaClass.getResource(name)?.toExternalForm()
  return if (resource != null) {
    resource
  }
  else {
    StyleManager.LOG.warn("Cannot find resource: $name")
    ""
  }
}

internal fun resourceFileName(): String {
  return when {
    isHighcontrast() -> "highcontrast"
    UIUtil.isUnderDarcula() -> "darcula"
    else -> "light"
  }
}

internal fun isHighcontrast() = LafManager.getInstance().currentLookAndFeel is UIThemeBasedLookAndFeelInfo