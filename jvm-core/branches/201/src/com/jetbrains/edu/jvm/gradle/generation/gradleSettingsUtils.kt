package com.jetbrains.edu.jvm.gradle.generation

import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

fun GradleProjectSettings.setDelegateBuildEnabled(enable: Boolean) {
  delegatedBuild = enable
}
