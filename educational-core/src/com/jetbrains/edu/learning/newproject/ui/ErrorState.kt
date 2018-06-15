package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.MessageType.*
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.getDisabledPlugins
import java.awt.Color

sealed class ErrorState(
  rawMessage: String?,
  val foregroundColor: Color,
  val hasLink: Boolean,
  val courseCanBeStarted: Boolean
) {

  val message: String? = rawMessage?.let(UIUtil::toHtml)

  object None : ErrorState(null, Color.BLACK, false, true)
  object NothingSelected : ErrorState(null, Color.BLACK, false, courseCanBeStarted = true)
  object NotLoggedIn : ErrorState("<u><b>Log in</b></u> to Stepik to see more courses", WARNING.titleForeground, true, courseCanBeStarted = true)
  object LoginRequired : ErrorState("<u><b>Log in</b></u> to Stepik to start this course", ERROR.titleForeground, true, courseCanBeStarted = false)
  object IncompatibleVersion : ErrorState("<u><b>Update</b></u> plugin to start this course", ERROR.titleForeground, true, courseCanBeStarted = false)
  data class RequiredPluginsDisabled(val disabledPluginIds: List<String>) :
    ErrorState("Some required plugins are disabled. <u><b>Enable plugins</b></u>", ERROR.titleForeground, true, courseCanBeStarted = false)

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

    private fun isLoggedIn(): Boolean = EduSettings.getInstance().user != null

    private fun isLoginRequired(selectedCourse: Course): Boolean =
      selectedCourse.isAdaptive || selectedCourse is RemoteCourse && !selectedCourse.isCompatible
  }
}

