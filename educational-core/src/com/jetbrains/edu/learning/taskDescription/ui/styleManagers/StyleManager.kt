package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJavaFxOrJCEF
import com.jetbrains.edu.learning.taskDescription.ui.EduToolsResourcesRequestHandler.Companion.resourceWebUrl
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager.Companion.LOG
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.BROWSER_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_BASE_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_DARCULA_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_HIGH_CONTRAST_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_LIGHT_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_MAC_LINUX_SHAPE_CSS
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.Companion.SCROLL_BARS_WIN_SHAPE_CSS
import kotlinx.css.Color
import java.net.URL

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

  private fun java.awt.Color.asCssColor(): Color = Color("#${ColorUtil.toHex(this)}")

  private fun bodyColor(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.message("darcula.body.color"))) else JBColor.foreground().asCssColor()
  }

  private fun codeBackground(): Color {
    return if (UIUtil.isUnderDarcula()) Color((TaskDescriptionBundle.message("darcula.code.background")))
    else Color(TaskDescriptionBundle.message("code.background"))
  }

  companion object {
    internal val LOG: Logger = Logger.getInstance(this::class.java)

    const val FONT_FACTOR_PROPERTY: String = "edu.task.description.font.factor"

    private val scrollBarsCssFileName: String
      get() = when {
        isHighContrast() -> SCROLL_BARS_HIGH_CONTRAST_CSS
        UIUtil.isUnderDarcula() -> SCROLL_BARS_DARCULA_CSS
        else -> SCROLL_BARS_LIGHT_CSS
      }

    val baseStylesheet: String
      get() = resourceUrl(BROWSER_CSS)

    val scrollBarStylesheetFiles: List<String>
      get() {
        return listOf(
          resourceUrl(SCROLL_BARS_BASE_CSS),
          if (SystemInfo.isWindows) resourceUrl(SCROLL_BARS_WIN_SHAPE_CSS) else resourceUrl(SCROLL_BARS_MAC_LINUX_SHAPE_CSS),
          resourceUrl(scrollBarsCssFileName)
        )
      }

    fun resources(content: String): Map<String, String> = StyleResourcesManager(content).resources
  }
}

/**
 * TODO refactor next resource-related methods
 *
 * JCEF doesn't load local resources
 * Otherwise JavaFX does load only local resources, see [javafx.scene.web.WebEngine.userStyleSheetLocation]
 */
fun resourceUrl(name: String): String = when {
  isJCEF() -> resourceWebUrl(name)
  else -> resourceFileUrl(name)
}

fun getResource(name: String): URL? = object {}.javaClass.getResource(name)

private fun resourceFileUrl(name: String): String {
  val resource = getResource(name)?.toExternalForm()
  return if (resource != null) {
    resource
  }
  else {
    LOG.warn("Cannot find resource: $name")
    ""
  }
}

internal fun isHighContrast(): Boolean {
  val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo ?: return false
  return lookAndFeel.theme.id == "JetBrainsHighContrastTheme"
}