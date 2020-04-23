package com.jetbrains.edu.javascript.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest

class JsHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val language: Language
    get() = JavascriptLanguage.INSTANCE

  override val settings: Any
    get() = JsNewProjectSettings()

  override val codeSample: String
    get() = """console.log(1)"""

  override val codeSampleWithHighlighting: String
    get() = """console.log(<span style="...">1</span>)"""
}