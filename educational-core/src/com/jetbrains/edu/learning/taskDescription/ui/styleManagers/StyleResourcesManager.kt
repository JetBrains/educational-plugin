package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.jetbrains.edu.learning.EduSettings
import kotlinx.css.*
import kotlinx.css.properties.lh

internal class StyleResourcesManager(taskText: String) {

  // update style/template.html.ft in case of changing key names
  val resources = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "content" to taskText,
    "base_css" to com.jetbrains.edu.learning.taskDescription.ui.loadText("/style/browser.css"),
    resourcePair("stepik_link", "/style/stepikLink.css"),
    *panelSpecificHintFiles()
  )

  private fun panelSpecificHintFiles(): Array<Pair<String, String>> {
    val isJavaFx = EduSettings.getInstance().shouldUseJavaFx()
    return if (isJavaFx) {
      arrayOf(
        resourcePair("jquery", ("/style/hint/javafx/jquery-1.9.1.js")),
        resourcePair("hint_base", "/style/hint/javafx/base.css"),
        resourcePair("hint_laf_specific", "/style/hint/javafx/${resourceFileName()}.css"),
        resourcePair("toggle_hint_script", "/style/hint/javafx/toggleHint.js"))
    }
    else {
      arrayOf(resourcePair("hint_base", "/style/hint/swing/base.css"))
    }
  }

  private fun resourcePair(name: String, path: String) = name to resourceUrl(path)

  private fun typographyAndColorStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      body {
        fontFamily = styleManager.bodyFont
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.bodyLineHeight.px.lh
        color = styleManager.bodyColor
        backgroundColor = styleManager.bodyBackground
      }

      ".code" {
        fontFamily = styleManager.codeFont
        backgroundColor = styleManager.codeBackground
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.codeFontSize.px else styleManager.codeFontSize.pt
      }

      ".code-block" {
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.codeLineHeight.px.lh
      }

      a {
        color = styleManager.linkColor
      }

      "::-webkit-scrollbar-corner" {
        backgroundColor = styleManager.bodyBackground
      }
    }.toString()
  }
}