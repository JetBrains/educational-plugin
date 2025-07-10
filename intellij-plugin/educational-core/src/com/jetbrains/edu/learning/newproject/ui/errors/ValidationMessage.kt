package com.jetbrains.edu.learning.newproject.ui.errors

import org.jetbrains.annotations.Nls

/**
 * Objects of this class store some message, either with an error or with information.
 * The messages usually have one hyperlink inside an `<a>` tag with an empty `href` attribute.
 * The hyperlink acts as a button for a user to fix the issue or directs to a webpage that helps to fix the issue.
 * The helping webpage in that case is specified in the [hyperlinkAddress].
 * Sometimes the links inside the `href` attribute are not empty, but they are always ignored.
 *
 * This class is intended to show messages inside [com.jetbrains.edu.learning.newproject.ui.errors.ErrorComponent], located
 * inside [com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel].
 *
 */
data class ValidationMessage(
  @Nls val message: String,
  val hyperlinkAddress: String? = null,
  val type: ValidationMessageType = ValidationMessageType.ERROR,
  val actionButtonText: String? = null,
  val action: (() -> Unit)? = null
)

fun ValidationMessage.ready(): SettingsValidationResult = SettingsValidationResult.Ready(this)
