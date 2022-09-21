package com.jetbrains.edu.learning.newproject.ui.errors

import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle

fun getErrorState(course: Course?, validateSettings: (Course) -> SettingsValidationResult): ErrorState {
  var languageError: ErrorState = ErrorState.NothingSelected
  if (course != null) {
    val validationResult = validateSettings(course)

    languageError = when (validationResult) {
      is SettingsValidationResult.Pending -> ErrorState.Pending
      is SettingsValidationResult.Ready -> {
        val validationMessage = validationResult.validationMessage
        validationMessage?.let { ErrorState.LanguageSettingsError(it) } ?: ErrorState.None
      }
    }
  }
  return ErrorState.forCourse(course).merge(languageError)
}

fun browseHyperlink(message: ValidationMessage?) {
  if (message == null) {
    return
  }
  val hyperlink = message.hyperlinkAddress
  if (hyperlink != null) {
    EduBrowser.getInstance().browse(hyperlink)
  }
}

fun getRequiredPluginsMessage(plugins: Collection<PluginInfo>): String {
  if (plugins.isEmpty()) {
    return ""
  }

  val names = plugins.map { it.displayName ?: it.stringId }
  return when (names.size) {
    1 -> EduCoreBundle.message("validation.plugins.required.plugins.one", names[0], EduNames.PLUGINS_HELP_LINK)
    2 -> EduCoreBundle.message("validation.plugins.required.plugins.two", names[0], names[1], EduNames.PLUGINS_HELP_LINK)
    3 -> EduCoreBundle.message("validation.plugins.required.plugins.three", names[0], names[1], names[2], EduNames.PLUGINS_HELP_LINK)
    else -> {
      val restPluginsNumber = plugins.size - 2
      EduCoreBundle.message("validation.plugins.required.plugins.more", names[0], names[1], restPluginsNumber, EduNames.PLUGINS_HELP_LINK)
    }
  }
}