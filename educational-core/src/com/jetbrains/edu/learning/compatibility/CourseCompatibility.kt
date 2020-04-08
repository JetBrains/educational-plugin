package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.extensions.PluginId

sealed class CourseCompatibility {
  object Compatible : CourseCompatibility()
  object IncompatibleVersion : CourseCompatibility()
  object Unsupported : CourseCompatibility()
  class PluginsRequired(val toInstallOrEnable: Set<PluginId>) : CourseCompatibility()
}
