package com.jetbrains.edu.learning.ui.taskDescription.styleManagers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import kotlinx.css.Color

class StyleManager {
  private val lafPrefix = if (UIUtil.isUnderDarcula()) "darcula" else "light"
  private val typographyManager = TypographyManager()

  val bodyFontSize = typographyManager.bodyFontSize
  val codeFontSize = typographyManager.codeFontSize
  val bodyLineHeight = typographyManager.bodyLineHeight
  val codeLineHeight = typographyManager.codeLineHeight
  val bodyFont = typographyManager.bodyFont
  val codeFont = typographyManager.codeFont

  val bodyColor = getCSSColor("$lafPrefix.body.color")
  val linkColor = getCSSColor("$lafPrefix.link.color")
  val bodyBackground = getCSSColor("$lafPrefix.body.background")
  val codeBackground = if (EduSettings.getInstance().shouldUseJavaFx()) bodyBackground
  else Color("#${ColorUtil.toHex(ColorUtil.dimmer(UIUtil.getPanelBackground()))}")

  val scrollBarStylesheets = getScrollBarStylesheetsUrls()
  val baseStylesheet = resourceUrl("/style/browser.css")
  val buttonStylesheets = listOfNotNull(baseStylesheet,
                                        resourceUrl(
                                          "/style/javafxButtons/buttonsBase.css"),
                                        resourceUrl(
                                          "/style/javafxButtons/buttonsDarcula.css").takeIf { UIUtil.isUnderDarcula() })

  fun resources(project: Project, content: String) = StyleResourcesManager(
    project, content).resources

  private fun getScrollBarStylesheetsUrls(): List<String> {
    return listOf(resourceUrl("/style/scrollbars/base.css"),
                  if (SystemInfo.isWindows) resourceUrl(
                    "/style/scrollbars/winShape.css")
                  else resourceUrl("/style/scrollbars/macLinuxShape.css"),
                  if (UIUtil.isUnderDarcula()) resourceUrl(
                    "/style/scrollbars/darcula.css")
                  else resourceUrl("/style/scrollbars/light.css"))
  }

  private fun getCSSColor(s: String): Color {
    return Color((TaskDescriptionBundle.message(s)))
  }

  companion object {
    internal val LOG = Logger.getInstance(this::class.java)
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