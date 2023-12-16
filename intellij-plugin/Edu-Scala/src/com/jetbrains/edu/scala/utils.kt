package com.jetbrains.edu.scala

import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.pluginVersion

val isScalaPluginCompatible: Boolean
  get() {
    val scalaPluginVersion = pluginVersion("org.intellij.scala") ?: return false
    return VersionComparatorUtil.compare(scalaPluginVersion, "2021.3.6") >= 0
  }
