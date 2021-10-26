package com.jetbrains.edu.scala

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.pluginVersion

val isScalaPluginCompatible: Boolean
  get() {
    val build = ApplicationInfo.getInstance().build
    // All Scala plugin versions for 212 builds and below are compatible with EduTools plugin
    if (build < BUILD_213) return true
    val scalaPluginVersion = pluginVersion("org.intellij.scala") ?: return false
    return VersionComparatorUtil.compare(scalaPluginVersion, "2021.3.6") >= 0
  }

// BACKCOMPAT: 2021.2
private val BUILD_213: BuildNumber = BuildNumber.fromString("213")!!
