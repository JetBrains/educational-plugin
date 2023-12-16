package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.learning.courseFormat.PluginInfo

sealed class CourseCompatibility {
  object Compatible : CourseCompatibility()
  object IncompatibleVersion : CourseCompatibility()
  object Unsupported : CourseCompatibility()
  class PluginsRequired(val toInstallOrEnable: List<PluginInfo>) : CourseCompatibility()
}
