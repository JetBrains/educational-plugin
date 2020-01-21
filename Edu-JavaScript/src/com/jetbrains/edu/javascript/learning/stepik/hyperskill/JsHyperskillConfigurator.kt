package com.jetbrains.edu.javascript.learning.stepik.hyperskill

import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class JsHyperskillConfigurator : HyperskillConfigurator<JsNewProjectSettings>(JsConfigurator()) {
  override fun getTestDirs() = listOf(HYPERSKILL_TEST_DIR)
}
