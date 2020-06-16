package com.jetbrains.edu.python.hyperskill

import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {

  override fun runTest() {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtils.isAndroidStudio()) {
      super.runTest()
    }
  }

  override val language: Language
    get() = PythonLanguage.INSTANCE

  override val settings: Any
    get() = PyNewProjectSettings()

  override val codeSample: String
    get() = """print(1)"""

  override val codeSampleWithHighlighting: String
    get() = """print(<span style="...">1</span>)"""
}
