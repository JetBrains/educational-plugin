package com.jetbrains.edu.go.stepik.hyperskill

import com.jetbrains.edu.go.GoConfigurator
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_GO_SUPPORT
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class GoHyperskillConfigurator : HyperskillConfigurator<GoProjectSettings>(GoConfigurator()) {
  override fun getMockFileName(text: String): String = MAIN_GO

  override val isEnabled: Boolean
    get() = isFeatureEnabled(HYPERSKILL_GO_SUPPORT)

  override val testDirs: List<String>
    get() = listOf()
}