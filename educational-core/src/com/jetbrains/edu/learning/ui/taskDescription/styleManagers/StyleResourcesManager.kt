package com.jetbrains.edu.learning.ui.taskDescription.styleManagers

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduLanguageDecorator
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.ui.taskDescription.loadText
import kotlinx.css.*
import kotlinx.css.properties.lh

internal class StyleResourcesManager(project: Project, taskText: String) {
  private fun decorator(project: Project): EduLanguageDecorator? {
    val language = StudyTaskManager.getInstance(project).course?.languageById
    return language?.let { EduLanguageDecorator.INSTANCE.forLanguage(language) }
  }

  // update style/template.html.ft in case of changing key names
  val resources = mapOf(
    "typography_color_style" to typographyAndColorStylesheet(),
    "language_script" to (decorator(project)?.languageScriptUrl ?: ""),
    "content" to taskText,
    "highlight_code" to highlightScript(project),
    "base_css" to loadText("/style/browser.css"),
    resourcePair("codemirror", "/code-mirror/codemirror.js"),
    resourcePair("jquery", ("/style/hint/jquery-1.9.1.js")),
    resourcePair("runmode", "/code-mirror/runmode.js"),
    resourcePair("colorize", "/code-mirror/colorize.js"),
    resourcePair("javascript", "/code-mirror/javascript.js"),
    resourcePair("hint_base", "/style/hint/base.css"),
    resourcePair("hint_laf_specific",  "/style/hint/${resourceFileName()}.css"),
    resourcePair("css_codemirror", "/code-mirror/${resourceFileName()}.css"),
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

  private fun highlightScript(project: Project): String {
    val loadText = loadText("/code-mirror/highlightCode.js.ft")
    return loadText?.replace("\${default_mode}", (decorator(project)?.defaultHighlightingMode ?: "")) ?: ""
  }
}