package com.jetbrains.edu.learning.ui.taskDescription.styleManagers

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.ui.taskDescription.loadText
import kotlinx.css.*
import kotlinx.css.properties.lh

internal class StyleResourcesManager(project: Project, taskText: String) {

  // update style/template.html.ft in case of changing key names
  val resources = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "content" to taskText,
    "base_css" to loadText("/style/browser.css"),
    resourcePair("jquery", ("/style/hint/jquery-1.9.1.js")),
    resourcePair("hint_base", "/style/hint/base.css"),
    resourcePair("hint_laf_specific",  "/style/hint/${resourceFileName()}.css"),
    resourcePair("toggle_hint_script", "/style/hint/toggleHint.js"),
    resourcePair("mathjax_script", "/style/mathjaxConfigure.js"),
    resourcePair("stepik_link", "/style/stepikLink.css")
  )

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

      code {
        fontFamily = styleManager.codeFont
        backgroundColor = styleManager.codeBackground
      }

      "pre code" {
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.codeFontSize.px else styleManager.codeFontSize.pt
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