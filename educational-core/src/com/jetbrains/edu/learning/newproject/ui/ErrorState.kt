package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.MessageType.ERROR
import com.intellij.openapi.ui.MessageType.WARNING
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checkio.CheckiOConfigurator
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.getDisabledPlugins
import java.awt.Color

sealed class ErrorState(
  private val severity: Int,
  val message: ErrorMessage?,
  val foregroundColor: Color,
  val courseCanBeStarted: Boolean
) {

  object NothingSelected : ErrorState(0, null, Color.BLACK, true)
  object None : ErrorState(1, null, Color.BLACK, true)
  object NotLoggedIn : ErrorState(2, ErrorMessage("", "Log in", " to Stepik to see more courses"), WARNING.titleForeground, true)
  object LoginRequired : ErrorState(3, ErrorMessage("", "Log in", " to Stepik to start this course"), ERROR.titleForeground, false)
  object CheckiOLoginRequired : ErrorState(3, ErrorMessage("", "Log in", " to CheckiO to start this course"), ERROR.titleForeground, false)
  object IncompatibleVersion : ErrorState(3, ErrorMessage("", "Update", " plugin to start this course"), ERROR.titleForeground, false)

  data class RequiredPluginsDisabled(val disabledPluginIds: List<String>) :
    ErrorState(3, errorMessage(disabledPluginIds), ERROR.titleForeground, false)
  class LanguageSettingsError(message: String) : ErrorState(3, ErrorMessage(message), ERROR.titleForeground, false)

  fun merge(other: ErrorState): ErrorState = if (severity < other.severity) other else this

  companion object {
    @JvmStatic
    fun forCourse(course: Course?): ErrorState {
      val pluginRequirements = course?.languageById?.let(EduConfiguratorManager::forLanguage)?.pluginRequirements().orEmpty()
      val disabledPlugins = getDisabledPlugins(pluginRequirements)
      return when {
        course == null -> NothingSelected
        course.compatibility !== CourseCompatibility.COMPATIBLE -> IncompatibleVersion
        disabledPlugins.isNotEmpty() -> RequiredPluginsDisabled(disabledPlugins)
        isCheckiOLoginRequired(course) -> CheckiOLoginRequired
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

    private fun isLoggedIn(): Boolean = EduSettings.getInstance().user != null

    private fun isLoginRequired(selectedCourse: Course): Boolean =
      selectedCourse.isAdaptive || selectedCourse is RemoteCourse && !selectedCourse.isCompatible

    private fun isCheckiOLoginRequired(selectedCourse: Course): Boolean {
      if (selectedCourse is CheckiOCourse) {
        val checkioConfigurator = selectedCourse.languageById.let(EduConfiguratorManager::forLanguage) as CheckiOConfigurator<*>
        val checkioAccountHolder = checkioConfigurator.oAuthConnector.accountHolder
        return !checkioAccountHolder.account.isLoggedIn
      }
      return false
    }
  }
}

data class ErrorMessage(val beforeLink: String, val link: String = "", val afterLink: String = "")
