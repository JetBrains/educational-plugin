package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.util.ThreeState
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

fun GradleProjectSettings.setDelegateBuildEnabled(enable: Boolean) {
  delegatedBuild = ThreeState.fromBoolean(enable)
}
