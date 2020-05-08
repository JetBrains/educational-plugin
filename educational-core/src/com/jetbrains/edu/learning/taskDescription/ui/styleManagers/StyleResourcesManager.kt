package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJavaFxOrJCEF
import com.jetbrains.edu.learning.taskDescription.ui.loadText
import kotlinx.css.*
import kotlinx.css.properties.lh

internal class StyleResourcesManager(taskText: String = "") {

  // update style/template.html.ft in case of changing key names
  val resources = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "content" to taskText,
    "base_css" to loadText(BROWSER_CSS),
    "mathJax" to "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML",
    resourcePair("stepik_link", STEPIK_LINK_CSS),
    resourcePair("codeforces_task", CODEFORCES_TASK_CSS),
    *panelSpecificHintFiles()
  ).plus(VideoTaskResourcesManager().videoResources)
    .plus(ChoiceTaskResourcesManager().choiceTaskResources)

  private fun panelSpecificHintFiles(): Array<Pair<String, String>> {
    return if (isJavaFxOrJCEF()) {
      arrayOf(
        resourcePair("jquery", JQUERY_JS),
        resourcePair("hint_base", JAVAFX_BASE_CSS),
        resourcePair("hint_laf_specific", hintLafSpecificFileName()),
        resourcePair("toggle_hint_script", JAVAFX_TOGGLE_HINT_JS)
      )
    }
    else {
      arrayOf(resourcePair("hint_base", SWING_BASE_CSS))
    }
  }

  private fun resourcePair(name: String, path: String) = name to resourceUrl(path)

  private fun typographyAndColorStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      body {
        fontFamily = styleManager.bodyFont
        fontSize = if (isJavaFxOrJCEF()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.bodyLineHeight.px.lh
        color = styleManager.bodyColor
        backgroundColor = styleManager.bodyBackground
      }

      ".code" {
        fontFamily = styleManager.codeFont
        backgroundColor = styleManager.codeBackground
        fontSize = if (isJavaFxOrJCEF()) styleManager.codeFontSize.px else styleManager.codeFontSize.pt
        padding = "4 4 4 4"
        borderRadius = 5.px
      }

      ".code-block" {
        fontSize = if (isJavaFxOrJCEF()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.codeLineHeight.px.lh
      }

      a {
        color = styleManager.linkColor
      }

      "::-webkit-scrollbar-corner" {
        backgroundColor = styleManager.bodyBackground
      }

      codeforcesCssAdjustment(styleManager)
    }.toString()
  }

  private fun codeforcesCssAdjustment(styleManager: StyleManager): CSSBuilder =
    CSSBuilder().apply {
      ".compact-problemset .problem-statement" {
        fontSize = styleManager.bodyFontSize.rem.times(1.4)
      }

      // Commented because otherwise all other font-sizes don't work ¯\_(ツ)_/¯
//      ".problem-statement" {
//        fontSize = styleManager.bodyFontSize.rem.times(1.4)
//      }

      ".problem-statement .header .title" {
        fontSize = styleManager.bodyFontSize.pct.times(150)
      }

      ".problem-statement .section-title" {
        fontSize = styleManager.bodyFontSize.pct.times(115)
      }

      // TODO remove next 2 items if inplace tests will be implemented
      ".problem-statement .sample-tests" {
        fontSize = styleManager.bodyFontSize.em.times(0.9)
      }

      ".problem-statement .sample-tests .title" {
        fontSize = styleManager.bodyFontSize.em.times(1.3)
      }
    }

  private fun hintLafSpecificFileName(): String {
    return when {
      isHighContrast() -> JAVAFX_HIGH_CONTRAST_CSS
      UIUtil.isUnderDarcula() -> JAVAFX_DARCULA_CSS
      else -> JAVAFX_LIGHT_CSS
    }
  }

  companion object {
    const val BROWSER_CSS: String = "/style/browser.css"
    const val CODEFORCES_TASK_CSS: String = "/style/codeforces_task.css"
    const val JAVAFX_BASE_CSS: String = "/style/hint/javafx/base.css"
    const val JAVAFX_DARCULA_CSS: String = "/style/hint/javafx/darcula.css"
    const val JAVAFX_HIGH_CONTRAST_CSS: String = "/style/hint/javafx/highcontrast.css"
    const val JAVAFX_LIGHT_CSS: String = "/style/hint/javafx/light.css"
    const val JAVAFX_TOGGLE_HINT_JS: String = "/style/hint/javafx/toggleHint.js"
    const val JQUERY_JS: String = "/style/hint/javafx/jquery-1.9.1.js"
    const val SCROLL_BARS_BASE_CSS: String = "/style/scrollbars/base.css"
    const val SCROLL_BARS_DARCULA_CSS: String = "/style/scrollbars/darcula.css"
    const val SCROLL_BARS_HIGH_CONTRAST_CSS: String = "/style/scrollbars/highcontrast.css"
    const val SCROLL_BARS_LIGHT_CSS: String = "/style/scrollbars/light.css"
    const val SCROLL_BARS_MAC_LINUX_SHAPE_CSS: String = "/style/scrollbars/macLinuxShape.css"
    const val SCROLL_BARS_WIN_SHAPE_CSS: String = "/style/scrollbars/winShape.css"
    const val STEPIK_LINK_CSS: String = "/style/stepikLink.css"
    const val SWING_BASE_CSS: String = "/style/hint/swing/base.css"

    val resourcesList = listOf(
      BROWSER_CSS,
      CODEFORCES_TASK_CSS,
      JAVAFX_BASE_CSS,
      JAVAFX_DARCULA_CSS,
      JAVAFX_HIGH_CONTRAST_CSS,
      JAVAFX_LIGHT_CSS,
      JAVAFX_TOGGLE_HINT_JS,
      JQUERY_JS,
      SCROLL_BARS_BASE_CSS,
      SCROLL_BARS_DARCULA_CSS,
      SCROLL_BARS_HIGH_CONTRAST_CSS,
      SCROLL_BARS_LIGHT_CSS,
      SCROLL_BARS_MAC_LINUX_SHAPE_CSS,
      SCROLL_BARS_WIN_SHAPE_CSS,
      STEPIK_LINK_CSS,
      SWING_BASE_CSS
    )
  }
}