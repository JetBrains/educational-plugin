package com.jetbrains.edu.learning.newproject.ui.errors

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.ext.compatibility
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorSeverity.*
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessageType.ERROR
import com.jetbrains.edu.learning.newproject.ui.getRequiredPluginsMessage
import com.jetbrains.edu.learning.ui.EduColors.errorTextForeground
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
  object Pending : ErrorState(LANGUAGE_SETTINGS_PENDING, null, Color.BLACK, false)

  abstract class LocationError(messageText: String) : ErrorState(LOCATION_ERROR,
                                                                 ValidationMessage(messageText, type = ERROR),
                                                                 errorTextForeground,
                                                                 false)

  object EmptyLocation : LocationError(EduCoreBundle.message("validation.empty.location"))
  object InvalidLocation : LocationError(EduCoreBundle.message("validation.cannot.create.course.at.location"))

  class CustomSevereError(message: String, val action: Runnable? = null) :
    ErrorState(LOGIN_ERROR, ValidationMessage(message), errorTextForeground, false)

  class LanguageSettingsError(message: ValidationMessage) : ErrorState(LANGUAGE_SETTINGS_ERROR, message, errorTextForeground, false)

  object JCEFRequired : ErrorState(NO_JCEF, ValidationMessage(
    EduCoreBundle.message("validation.no.jcef")), errorTextForeground, false)

  object IncompatibleVersion : ErrorState(PLUGIN_UPDATE_REQUIRED, ValidationMessage(EduCoreBundle.message("validation.plugins.required")),
                                          errorTextForeground, false)

  data class RequirePlugins(val pluginIds: List<PluginInfo>) : ErrorState(PLUGIN_UPDATE_REQUIRED, errorMessageForRequiredPlugins(pluginIds),
                                                                          errorTextForeground, false)

  object RestartNeeded : ErrorState(PLUGIN_UPDATE_REQUIRED,
                                    ValidationMessage(EduCoreBundle.message("validation.plugins.restart.to.activate.plugin")),
                                    errorTextForeground,
                                    false)

  class UnsupportedCourse(@Nls(capitalization = Nls.Capitalization.Sentence) message: String) : ErrorState(UNSUPPORTED_COURSE,
                                                                                                           ValidationMessage(message),
                                                                                                           errorTextForeground, false)

  fun merge(other: ErrorState): ErrorState = if (severity < other.severity) other else this

  companion object {
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

    private val Course.unsupportedCourseMessage: String
      @Nls(capitalization = Nls.Capitalization.Sentence)
      get() {
        val type = when (val environment = course.environment) {
          EduNames.ANDROID -> environment
          DEFAULT_ENVIRONMENT -> course.languageDisplayName
          else -> null
        }
        return if (type != null) {
          EduCoreBundle.message("courses.not.supported", type)
        }
        else {
          EduCoreBundle.message("selected.course.not.supported", course.name)
        }
      }

    private val Course.courseSpecificError: ErrorState
      get() {
        return when {
          CoursesStorage.getInstance().hasCourse(this) -> None
          else -> None
        }
      }

    private fun errorMessageForRequiredPlugins(plugins: Collection<PluginInfo>): ValidationMessage {
      return ValidationMessage(getRequiredPluginsMessage(plugins, actionAsLink = true))
    }

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

  }
}

/**
 * Order is meaningful. Errors with more priority are located lower
 */
private enum class ErrorSeverity {
  OK,

  LANGUAGE_SETTINGS_PENDING,

  LOGIN_RECOMMENDED,
  LOGIN_ERROR,

  LANGUAGE_SETTINGS_ERROR,
  LOCATION_ERROR,

  NO_JCEF,

  PLUGIN_UPDATE_REQUIRED,

  UNSUPPORTED_COURSE
}

