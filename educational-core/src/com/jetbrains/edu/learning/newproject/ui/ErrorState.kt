package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.codeforces.api.ContestInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType.ERROR
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType.WARNING
import com.jetbrains.edu.learning.plugins.PluginInfo
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors.errorTextForeground
import com.jetbrains.edu.learning.ui.EduColors.warningTextForeground
import java.awt.Color

sealed class ErrorState(
  private val severity: Int,
  val message: ValidationMessage?,
  val foregroundColor: Color,
  val courseCanBeStarted: Boolean
) {

  object NothingSelected : ErrorState(0, null, Color.BLACK, false)
  object None : ErrorState(1, null, Color.BLACK, true)
  object NotLoggedIn : ErrorState(2, ValidationMessage("", "Log in", " to Stepik to see more courses", type = WARNING), warningTextForeground, true)
  abstract class LoginRequired(platformName: String) : ErrorState(3, ValidationMessage("", "Log in", " to $platformName to start this course"), errorTextForeground, false)
  object StepikLoginRequired : LoginRequired(StepikNames.STEPIK)
  class CheckiOLoginRequired(courseName: String) : LoginRequired(courseName) // Name of CheckiO course equals corresponding CheckiO platform name
  //TODO: remove it?
  object HyperskillLoginRequired : LoginRequired(EduNames.JBA)
  object IncompatibleVersion : ErrorState(3, ValidationMessage("", "Update", " plugin to start this course"), errorTextForeground, false)
  class UnsupportedCourse(message: String) : ErrorState(3, ValidationMessage(message), errorTextForeground, false)
  data class RequirePlugins(val pluginIds: List<PluginInfo>) :
    ErrorState(3, errorMessage(pluginIds), errorTextForeground, false)
  object RestartNeeded : ErrorState(3, ValidationMessage("", "Restart", " to activate plugin updates"), errorTextForeground, false)
  class LanguageSettingsError(message: ValidationMessage) : ErrorState(3, message, errorTextForeground, false)
  object JavaFXRequired : ErrorState(4, ValidationMessage("No JavaFX found. Please ", "switch", " to JetBrains Runtime to start the course"), errorTextForeground, false)
  class CustomSevereError(beforeLink: String, link: String = "", afterLink: String = "", val action: Runnable? = null) :
    ErrorState(3, ValidationMessage(beforeLink, link, afterLink), errorTextForeground, false)
  object JetBrainsAcademyLoginRecommended : ErrorState(2, ValidationMessage("", linkText = "Log in", afterLink = " to existing ${EduNames.JBA} account to open a project"), warningTextForeground, true)

  fun merge(other: ErrorState): ErrorState = if (severity < other.severity) other else this

  companion object {
    @JvmStatic
    fun forCourse(course: Course?): ErrorState {
      val courseCompatibility = course?.compatibility
      return when {
        course == null -> NothingSelected
        course is JetBrainsAcademyCourse -> if (HyperskillSettings.INSTANCE.account == null) JetBrainsAcademyLoginRecommended else None
        courseCompatibility is CourseCompatibility.PluginsRequired -> {
          if (courseCompatibility.toInstallOrEnable.isEmpty()) RestartNeeded else RequirePlugins(courseCompatibility.toInstallOrEnable)
        }
        courseCompatibility == CourseCompatibility.IncompatibleVersion -> IncompatibleVersion
        courseCompatibility == CourseCompatibility.Unsupported -> UnsupportedCourse(course.unsupportedCourseMessage)
        course is CourseraCourse -> None
        course is CheckiOCourse -> getCheckiOError(course)
        course is HyperskillCourse -> if (HyperskillSettings.INSTANCE.account == null) HyperskillLoginRequired else None
        course is ContestInfo -> None
        !isLoggedInToStepik() -> if (isStepikLoginRequired(course)) StepikLoginRequired else NotLoggedIn
        else -> None
      }
    }

    private fun getCheckiOError(course: Course): ErrorState {
      if (!EduUtils.hasJavaFx()) {
        return JavaFXRequired
      }
      return if (isCheckiOLoginRequired(course)) CheckiOLoginRequired(course.name) else None
    }

    private fun errorMessage(plugins: Collection<PluginInfo>, limit: Int = 3): ValidationMessage {
      require(limit > 1)

      val names = if (plugins.size == 1) {
        plugins.single().displayName
      }
      else {
        val suffix = if (plugins.size <= limit) " and ${plugins.last().displayName}" else " and ${plugins.size - limit + 1} more"
        plugins.take(minOf(limit - 1, plugins.size - 1)).joinToString { it.displayName } + suffix
      }

      val message = "$names ${StringUtil.pluralize("plugin", plugins.size)} required. "
      return ValidationMessage(message, EduCoreBundle.message("course.dialog.error.plugin.install.and.enable"), "")
    }

    @JvmStatic
    fun errorMessage(disabledPluginIds: Collection<PluginId>): ValidationMessage {
      val pluginName = if (disabledPluginIds.size == 1) {
        PluginManager.getPlugin(disabledPluginIds.first())?.name
      } else {
        null
      }
      val beforeLink = if (pluginName != null) {
        "Required \"$pluginName\" plugin is disabled. "
      } else {
        "Some required plugins are not installed or disabled. "
      }
      return ValidationMessage(beforeLink, EduCoreBundle.message("course.dialog.error.plugin.install.and.enable"), "")
    }

    private fun isLoggedInToStepik(): Boolean = EduSettings.isLoggedIn()

    private fun isStepikLoginRequired(selectedCourse: Course): Boolean =
      selectedCourse is EduCourse && selectedCourse.isRemote && !selectedCourse.isCompatible

    private fun isCheckiOLoginRequired(selectedCourse: Course): Boolean {
      if (selectedCourse is CheckiOCourse) {
        val checkiOConnectorProvider = selectedCourse.configurator as CheckiOConnectorProvider
        val checkiOAccount = checkiOConnectorProvider.oAuthConnector.account
        return checkiOAccount == null
      }
      return false
    }
  }
}

data class ValidationMessage @JvmOverloads constructor(
  val beforeLink: String,
  val linkText: String = "",
  val afterLink: String = "",
  val hyperlinkAddress: String? = null,
  val type: ValidationMessageType = ERROR
)

enum class ValidationMessageType {
  WARNING,
  ERROR
}