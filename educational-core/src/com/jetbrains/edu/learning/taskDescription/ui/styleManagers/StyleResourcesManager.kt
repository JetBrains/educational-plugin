package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.taskDescription.ui.EduToolsResourcesRequestHandler
import java.net.URL

object StyleResourcesManager {
  private val LOG: Logger = Logger.getInstance(this::class.java)

  private const val BROWSER_CSS: String = "/style/browser.css"
  private const val CODEFORCES_TASK_CSS: String = "/style/codeforces_task.css"
  const val EXTERNAL_LINK_ARROW_PNG = "/icons/com/jetbrains/edu/learning/external_link_arrow@2x.png"
  const val EXTERNAL_LINK_ARROW_DARK_PNG = "/icons/com/jetbrains/edu/learning/external_link_arrow@2x_dark.png"

  private const val HINT_BASE_CSS: String = "/style/hint/base.css"
  private const val HINT_SWING_BASE_CSS: String = "/style/hint/swing/base.css"
  private const val HINT_DARCULA_CSS: String = "/style/hint/darcula.css"
  private const val HINT_HIGH_CONTRAST_CSS: String = "/style/hint/highcontrast.css"
  private const val HINT_LIGHT_CSS: String = "/style/hint/light.css"
  private const val TOGGLE_HINT_JS: String = "/style/hint/toggleHint.js"
  private const val JQUERY_JS: String = "/style/hint/jquery-1.9.1.js"

  private const val SCROLL_BARS_BASE: String = "/style/scrollbars/base.css"
  private const val SCROLL_BARS_DARCULA_CSS: String = "/style/scrollbars/darcula.css"
  private const val SCROLL_BARS_HIGH_CONTRAST_CSS: String = "/style/scrollbars/highcontrast.css"
  private const val SCROLL_BARS_LIGHT_CSS: String = "/style/scrollbars/light.css"

  private const val STEPIK_LINK_CSS: String = "/style/stepikLink.css"

  private const val INTELLIJ_ICON_FONT_EOT: String = "/style/hint/fonts/intellij-icon-font.eot"
  private const val INTELLIJ_ICON_FONT_SVG: String = "/style/hint/fonts/intellij-icon-font.svg"
  private const val INTELLIJ_ICON_FONT_TTF: String = "/style/hint/fonts/intellij-icon-font.ttf"
  private const val INTELLIJ_ICON_FONT_WOFF: String = "/style/hint/fonts/intellij-icon-font.woff"
  private const val INTELLIJ_ICON_FONT_DARCULA_EOT: String = "/style/hint/fonts/intellij-icon-font-darcula.eot"
  private const val INTELLIJ_ICON_FONT_DARCULA_SVG: String = "/style/hint/fonts/intellij-icon-font-darcula.svg"
  private const val INTELLIJ_ICON_FONT_DARCULA_TTF: String = "/style/hint/fonts/intellij-icon-font-darcula.ttf"
  private const val INTELLIJ_ICON_FONT_DARCULA_WOFF: String = "/style/hint/fonts/intellij-icon-font-darcula.woff"

  val resourcesList = listOf(
    BROWSER_CSS,
    CODEFORCES_TASK_CSS,
    EXTERNAL_LINK_ARROW_PNG,
    EXTERNAL_LINK_ARROW_DARK_PNG,
    HINT_BASE_CSS,
    HINT_SWING_BASE_CSS,
    HINT_DARCULA_CSS,
    HINT_HIGH_CONTRAST_CSS,
    HINT_LIGHT_CSS,
    TOGGLE_HINT_JS,
    JQUERY_JS,
    SCROLL_BARS_BASE,
    SCROLL_BARS_DARCULA_CSS,
    SCROLL_BARS_HIGH_CONTRAST_CSS,
    SCROLL_BARS_LIGHT_CSS,
    STEPIK_LINK_CSS,
    INTELLIJ_ICON_FONT_EOT,
    INTELLIJ_ICON_FONT_SVG,
    INTELLIJ_ICON_FONT_TTF,
    INTELLIJ_ICON_FONT_WOFF,
    INTELLIJ_ICON_FONT_DARCULA_EOT,
    INTELLIJ_ICON_FONT_DARCULA_SVG,
    INTELLIJ_ICON_FONT_DARCULA_TTF,
    INTELLIJ_ICON_FONT_DARCULA_WOFF
  )

  private val panelSpecificHintResources: Map<String, String>
    get() = if (isJCEF()) {
      mapOf(
        "jquery" to resourceUrl(JQUERY_JS),
        "hint_base" to resourceUrl(HINT_BASE_CSS),
        "hint_laf_specific" to resourceUrl(hintLafSpecificFileName),
        "toggle_hint_script" to resourceUrl(TOGGLE_HINT_JS)
      )
    }
    else {
      mapOf("hint_base" to HINT_SWING_BASE_CSS)
    }

  private val hintLafSpecificFileName: String
    get() = when {
      isHighContrast() -> HINT_HIGH_CONTRAST_CSS
      UIUtil.isUnderDarcula() -> HINT_DARCULA_CSS
      else -> HINT_LIGHT_CSS
    }

  private val scrollbarLafSpecific: String
    get() = when {
      isHighContrast() -> SCROLL_BARS_HIGH_CONTRAST_CSS
      UIUtil.isUnderDarcula() -> SCROLL_BARS_DARCULA_CSS
      else -> SCROLL_BARS_LIGHT_CSS
    }

  // update style/template.html.ft in case of changing key names
  fun getResources(taskText: String = "") = mapOf(
    resourcePair("base_css", BROWSER_CSS),
    "typography_color_style" to StyleManager().typographyAndColorStylesheet(),
    "content" to taskText,
    "mathJax" to "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML",
    resourcePair("stepik_link", STEPIK_LINK_CSS),
    resourcePair("codeforces_task", CODEFORCES_TASK_CSS),
    resourcePair("scrollbar_style_laf", scrollbarLafSpecific),
    resourcePair("scrollbar_style_base", SCROLL_BARS_BASE)
  )
    .plus(panelSpecificHintResources)
    .plus(VideoTaskResourcesManager().videoResources)
    .plus(ChoiceTaskResourcesManager().choiceTaskResources)

  private fun resourcePair(name: String, path: String) = name to resourceUrl(path)

  /**
   * JCEF doesn't load local resources, otherwise let's load as local resources
   */
  fun resourceUrl(name: String): String = when {
    isJCEF() -> EduToolsResourcesRequestHandler.resourceWebUrl(name)
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

  fun isHighContrast(): Boolean {
    val lookAndFeel = LafManager.getInstance().currentLookAndFeel as? UIThemeBasedLookAndFeelInfo ?: return false
    return lookAndFeel.theme.id == "JetBrainsHighContrastTheme"
  }
}