package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.installAndEnablePlugin
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.getRequiredPluginsMessage

/**
 * This class is used to store error messages that might occur when the user opens a project from a browser.
 * This is possible for Stepik, Hyperskill, or Marketplace with actions such as “Solve in IDE” or “Open in IDE”.
 *
 * Validation results have a textual message for the user and sometimes may have an additional action that helps to fix the issue.
 * For example, an action to install or enable missing plugins, an action to view a help page.
 */
sealed class CourseValidationResult {
  abstract val message: String
}

/**
 * This validation result means that not enough plugins are installed and enabled and contains a list of missing plugins.
 */
data class PluginsRequired(val pluginIds: List<PluginInfo>) : CourseValidationResult() {
  override val message: String
    get() = getRequiredPluginsMessage(pluginIds, actionAsLink = false)

  fun actionText(): String = EduCoreBundle.message("validation.plugins.required.plugins.action")

  fun showPluginInstallAndEnableDialog() {
    val pluginStringIds = pluginIds.mapTo(HashSet()) { PluginId.getId(it.stringId) }
    installAndEnablePlugin(pluginStringIds) {}
  }
}

/**
 * This validation result means that there is some error; it is explained in the text with hyperlinks to help pages.
 * Unfortunately, platform notifications deprecate hyperlink listeners, so it is not good enough to use such messages
 * inside platform notifications.
 *
 * Such messages should better be reworded to exclude hyperlinks from the text and offer instead a "see more" action that
 * navigates to a help page online.
 */
@Deprecated("Reword the message to have no hyperlinks inside the text. Offer a 'see more' action instead of a link.")
data class ValidationErrorMessageWithHyperlinks(override val message: String) : CourseValidationResult()

/**
 * This validation result means that there is some error, but it is only possible to explain it in plain text,
 * and no automatic resolution is possible.
 */
data class ValidationErrorMessage(override val message: String) : CourseValidationResult()
