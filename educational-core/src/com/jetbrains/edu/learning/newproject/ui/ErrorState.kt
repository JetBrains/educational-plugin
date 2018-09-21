package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.MessageType.ERROR
import com.intellij.openapi.ui.MessageType.WARNING
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.courseFormat.ext.isAdaptive
import com.jetbrains.edu.learning.courseFormat.ext.isCompatible
import com.jetbrains.edu.learning.getDisabledPlugins
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
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
  abstract class LoginRequired(platformName: String) : ErrorState(3, ErrorMessage("", "Log in", " to $platformName to start this course"), ERROR.titleForeground, false)
  object StepikLoginRequired : LoginRequired(StepikNames.STEPIK)
  object CheckiOLoginRequired : LoginRequired(CheckiONames.CHECKIO)
  object HyperskillLoginRequired : LoginRequired("Hyperskill")
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
        course.courseType == CourseraNames.COURSE_TYPE -> None
        isCheckiOLoginRequired(course) -> CheckiOLoginRequired
        course is HyperskillCourse -> if (HyperskillSettings.INSTANCE.account == null) HyperskillLoginRequired else None
        !isLoggedInToStepik() -> if (isStepikLoginRequired(course)) StepikLoginRequired else NotLoggedIn
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

    private fun isLoggedInToStepik(): Boolean = EduSettings.isLoggedIn()

    private fun isStepikLoginRequired(selectedCourse: Course): Boolean =
      selectedCourse is StepikCourse && !selectedCourse.isCompatible

    private fun isCheckiOLoginRequired(selectedCourse: Course): Boolean {
      if (selectedCourse is CheckiOCourse) {
        val checkiOConnectorProvider = selectedCourse.languageById.let(EduConfiguratorManager::forLanguage) as CheckiOConnectorProvider
        val checkiOAccount = checkiOConnectorProvider.oAuthConnector.account
        return checkiOAccount == null
      }
      return false
    }
  }
}

data class ErrorMessage(val beforeLink: String, val link: String = "", val afterLink: String = "")
