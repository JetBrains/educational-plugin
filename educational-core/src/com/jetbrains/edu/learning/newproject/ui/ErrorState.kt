package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.ErrorSeverity.*
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType.ERROR
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType.WARNING
import com.jetbrains.edu.learning.plugins.PluginInfo
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.ui.EduColors.errorTextForeground
import com.jetbrains.edu.learning.ui.EduColors.warningTextForeground
import org.jetbrains.annotations.Nls
import java.awt.Color

sealed class ErrorState(
  private val severity: ErrorSeverity,
  val message: ValidationMessage?,
  val foregroundColor: Color,
  val courseCanBeStarted: Boolean
) {

  object NothingSelected : ErrorState(OK, null, Color.BLACK, false)
  object None : ErrorState(OK, null, Color.BLACK, true)

  object NotLoggedIn : ErrorState(LOGIN_RECOMMENDED,
                                  ValidationMessage(EduCoreBundle.message("validation.stepik.log.in.needed"), type = WARNING),
                                  warningTextForeground,
                                  true)

  object JetBrainsAcademyLoginNeeded : ErrorState(LOGIN_ERROR,
                                                  ValidationMessage(EduCoreBundle.message("validation.hyperskill.login.needed")),
                                                  errorTextForeground,
                                                  false)

  abstract class LocationError(messageText: String) : ErrorState(LOCATION_ERROR,
                                                                 ValidationMessage(messageText, type = ERROR),
                                                                 errorTextForeground,
                                                                 false)

  object EmptyLocation : LocationError(EduCoreBundle.message("validation.empty.location"))
  object InvalidLocation : LocationError(EduCoreBundle.message("validation.cannot.create.course.at.location"))

  abstract class LoginRequired(
    platformName: String
  ) : ErrorState(LOGIN_ERROR,
                 ValidationMessage(EduCoreBundle.message("validation.log.in.to.start.course", platformName)),
                 errorTextForeground,
                 false)

  object StepikLoginRequired : LoginRequired(StepikNames.STEPIK)

  // Name of CheckiO course equals corresponding CheckiO platform name
  class CheckiOLoginRequired(courseName: String) : LoginRequired(courseName)

  //TODO: remove it?
  object HyperskillLoginRequired : LoginRequired(EduNames.JBA)
  class CustomSevereError(message: String, val action: Runnable? = null) :
    ErrorState(LOGIN_ERROR, ValidationMessage(message), errorTextForeground, false)

  class LanguageSettingsError(message: ValidationMessage) : ErrorState(LANGUAGE_SETTINGS_ERROR, message, errorTextForeground, false)

  object JCEFRequired : ErrorState(NO_JCEF, ValidationMessage(
    EduCoreBundle.message("validation.no.jcef")), errorTextForeground, false)

  object IncompatibleVersion : ErrorState(PLUGIN_UPDATE_REQUIRED, ValidationMessage(EduCoreBundle.message("validation.plugin.required")),
                                          errorTextForeground, false)

  data class RequirePlugins(val pluginIds: List<PluginInfo>) : ErrorState(PLUGIN_UPDATE_REQUIRED, errorMessage(pluginIds),
                                                                          errorTextForeground, false)

  object RestartNeeded : ErrorState(PLUGIN_UPDATE_REQUIRED,
                                    ValidationMessage(EduCoreBundle.message("validation.plugins.restart.to.activate.plugin")),
                                    errorTextForeground,
                                    false)

  class UnsupportedCourse(message: String) : ErrorState(UNSUPPORTED_COURSE, ValidationMessage(message), errorTextForeground, false)

  fun merge(other: ErrorState): ErrorState = if (severity < other.severity) other else this

  companion object {
    @JvmStatic
    fun forCourse(course: Course?): ErrorState {
      if (course == null) return NothingSelected
      return None
        .merge(course.compatibilityError)
        .merge(course.courseSpecificError)
    }

    private val Course.compatibilityError: ErrorState
      get() {
        return when (val compatibility = compatibility) {
          CourseCompatibility.Unsupported -> UnsupportedCourse(unsupportedCourseMessage)
          CourseCompatibility.IncompatibleVersion -> IncompatibleVersion
          is CourseCompatibility.PluginsRequired -> {
            if (compatibility.toInstallOrEnable.isEmpty()) RestartNeeded else RequirePlugins(compatibility.toInstallOrEnable)
          }
          else -> None
        }
      }

    private val Course.courseSpecificError: ErrorState
      get() {
        return when (this) {
          is CheckiOCourse -> checkiOError
          is JetBrainsAcademyCourse -> if (HyperskillSettings.INSTANCE.account == null) JetBrainsAcademyLoginNeeded else None
          is HyperskillCourse -> if (HyperskillSettings.INSTANCE.account == null) HyperskillLoginRequired else None
          is EduCourse -> {
            if (!isMarketplace && !isLoggedInToStepik()) {
              if (isStepikLoginRequired(this)) StepikLoginRequired else NotLoggedIn
            }
            else {
              None
            }
          }
          else -> None
        }
      }

    private val CheckiOCourse.checkiOError: ErrorState
      get() {
        if (!isJCEF()) {
          return JCEFRequired
        }
        return if (isCheckiOLoginRequired(this)) CheckiOLoginRequired(name) else None
      }

    private fun errorMessage(plugins: Collection<PluginInfo>, limit: Int = 3): ValidationMessage {
      val message = getRequiredPluginsMessage(plugins, limit)
      return ValidationMessage(message + EduCoreBundle.message("course.dialog.error.plugin.install.and.enable"))
    }

    @JvmStatic
    fun errorMessage(disabledPluginIds: Collection<PluginId>): ValidationMessage {
      val pluginName = if (disabledPluginIds.size == 1) {
        PluginManagerCore.getPlugin(disabledPluginIds.first())?.name
      }
      else {
        null
      }
      return if (pluginName != null) {
        ValidationMessage(EduCoreBundle.message("validation.plugins.disabled.plugin", pluginName))
      }
      else {
        ValidationMessage(EduCoreBundle.message("validation.plugins.disabled.or.not.installed.plugins"))
      }
    }

    private fun isLoggedInToStepik(): Boolean = EduSettings.isLoggedIn()

    private fun isStepikLoginRequired(selectedCourse: EduCourse): Boolean =
      selectedCourse.isStepikRemote && !selectedCourse.isCompatible

    private fun isCheckiOLoginRequired(selectedCourse: CheckiOCourse): Boolean {
      if (CoursesStorage.getInstance().hasCourse(selectedCourse)) {
        return false
      }
      val checkiOConnectorProvider = selectedCourse.configurator as? CheckiOConnectorProvider ?: return false
      return !checkiOConnectorProvider.oAuthConnector.isLoggedIn()
    }
  }
}

/**
 * Order is meaningful. Errors with more priority are located lower
 */
private enum class ErrorSeverity {
  OK,

  LOGIN_RECOMMENDED,
  LOGIN_ERROR,

  LANGUAGE_SETTINGS_ERROR,
  LOCATION_ERROR,

  NO_JCEF,

  PLUGIN_UPDATE_REQUIRED,

  UNSUPPORTED_COURSE
}

data class ValidationMessage @JvmOverloads constructor(
  @Nls val message: String,
  val hyperlinkAddress: String? = null,
  val type: ValidationMessageType = ERROR
)

enum class ValidationMessageType {
  WARNING,
  ERROR,
  INFO
}