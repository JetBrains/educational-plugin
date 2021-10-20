package com.jetbrains.edu.go.hyperskill

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_GO_SUPPORT
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest
import com.jetbrains.edu.learning.withFeature

class GoHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val language: Language
    get() = GoLanguage.INSTANCE

  override val settings: Any
    get() = GoProjectSettings(GoSdk.NULL)

  override val codeSample: String
    get() = """fun main() {}"""

  override val codeSampleWithHighlighting: String
    get() = """<span style="...">fun main</span><span style="...">() {}</span>"""

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(HYPERSKILL_GO_SUPPORT, true) {
      super.runTestRunnable(context)
    }
  }

}
