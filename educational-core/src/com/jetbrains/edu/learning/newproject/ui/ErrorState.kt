package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.MessageType.ERROR
import com.intellij.openapi.ui.MessageType.WARNING
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.getDisabledPlugins
import com.jetbrains.edu.learning.stepik.StepikUtils.isLoggedIn
import java.awt.Color

sealed class ErrorState(
  val message: ErrorMessage?,
  val foregroundColor: Color,
  val courseCanBeStarted: Boolean
) {

  object None : ErrorState(null, Color.BLACK, true)
  object NothingSelected : ErrorState(null, Color.BLACK, true)
  object NotLoggedIn : ErrorState(ErrorMessage("", "Log in", " to Stepik to see more courses"), WARNING.titleForeground, true)
  object LoginRequired : ErrorState(ErrorMessage("", "Log in", " to Stepik to start this course"), ERROR.titleForeground, false)
  object IncompatibleVersion : ErrorState(ErrorMessage("", "Update", " plugin to start this course"), ERROR.titleForeground, false)
  data class RequiredPluginsDisabled(val disabledPluginIds: List<String>) :
    ErrorState(errorMessage(disabledPluginIds), ERROR.titleForeground, false)

  companion object {
    @JvmStatic
    fun forCourse(course: Course?): ErrorState {
      val pluginRequirements = course?.languageById?.let(EduConfiguratorManager::forLanguage)?.pluginRequirements().orEmpty()
      val disabledPlugins = getDisabledPlugins(pluginRequirements)
      return when {
        course == null -> NothingSelected
        course.compatibility !== CourseCompatibility.COMPATIBLE -> IncompatibleVersion
        disabledPlugins.isNotEmpty() -> RequiredPluginsDisabled(disabledPlugins)
        !isLoggedIn() -> if (isLoginRequired(course)) LoginRequired else NotLoggedIn
        else -> None
      }
    }

    @JvmStatic
    fun errorMessage(disabledPluginIds: List<String>): ErrorMessage {
      val pluginName = if (disabledPluginIds.size == 1) {
        PluginManager.getPlugin(PluginId.getId(disabledPluginIds[0]))?.name
      } else {
        null
      }
      val beforeLink = if (pluginName != null) {
        "Required \"$pluginName\" plugin is disabled. "
      } else {
        "Some required plugins are disabled. "
      }
      return ErrorMessage(beforeLink, "Enable", "")
    }

    private fun isLoginRequired(selectedCourse: Course): Boolean =
      selectedCourse.isAdaptive || selectedCourse is RemoteCourse && !selectedCourse.isCompatible
  }
}

data class ErrorMessage(val beforeLink: String, val link: String = "", val afterLink: String = "")
