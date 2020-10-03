package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.plugins.PluginInfo
import icons.EducationalCoreIcons
import java.awt.Component
import javax.swing.Icon

private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.newproject.ui.utils")

val Course.logo: Icon?
  get() {
    if (this is JetBrainsAcademyCourse) {
      return EducationalCoreIcons.JB_ACADEMY_TAB
    }
    val logo = configurator?.logo ?: compatibilityProvider?.logo
    if (logo == null) {
      val language = languageDisplayName
      LOG.info("configurator and compatibilityProvider are null. language: $language, course type: $itemType, environment: $environment")
    }

    return logo
  }

fun Course.getScaledLogo(logoSize: Int, ancestor: Component): Icon? {
  val logo = logo ?: return null
  val scaleFactor = logoSize / logo.iconHeight.toFloat()
  val scaledIcon = IconUtil.scale(logo, ancestor, scaleFactor)
  return IconUtil.toSize(scaledIcon, JBUI.scale(logoSize), JBUI.scale(logoSize))
}

val Course.unsupportedCourseMessage: String get() {
  val type = when (val environment = course.environment) {
    EduNames.ANDROID -> environment
    EduNames.DEFAULT_ENVIRONMENT -> course.languageDisplayName
    else -> null
  }
  return if (type != null) {
    "$type courses are not supported"
  } else {
    """Selected "${course.name}" course is unsupported"""
  }
}

fun getErrorState(course: Course?, validateSettings: (Course) -> ValidationMessage?): ErrorState {
  var languageError: ErrorState = ErrorState.NothingSelected
  if (course != null) {
    val languageSettingsMessage = validateSettings(course)
    languageError = languageSettingsMessage?.let { ErrorState.LanguageSettingsError(it) } ?: ErrorState.None
  }
  return ErrorState.forCourse(course).merge(languageError)
}

fun getRequiredPluginsMessage(plugins: Collection<PluginInfo>, limit: Int = 3): String {
  require(limit > 1)

  val names = if (plugins.size == 1) {
    plugins.single().displayName
  }
  else {
    val suffix = if (plugins.size <= limit) " and ${plugins.last().displayName}" else " and ${plugins.size - limit + 1} more"
    plugins.take(minOf(limit - 1, plugins.size - 1)).joinToString { it.displayName } + suffix
  }

  return "$names ${StringUtil.pluralize("plugin", plugins.size)} required. "
}

fun browseHyperlink(message: ValidationMessage?) {
  if (message == null) {
    return
  }
  val hyperlink = message.hyperlinkAddress
  if (hyperlink != null) {
    EduBrowser.browse(hyperlink)
  }
}
