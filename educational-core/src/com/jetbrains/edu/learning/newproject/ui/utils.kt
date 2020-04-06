package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import java.awt.Component
import javax.swing.Icon

private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.newproject.ui.utils")
private const val INITIAL_LOGO_SIZE = 16f

val Course.logo: Icon?
  get() {
    val configurator = course.configurator
    if (configurator == null) {
      val languageName = course.languageDisplayName
      LOG.info(String.format("configurator is null, language: %s course type: %s", languageName, course.itemType))
      return null
    }
    return configurator.logo
  }

fun Course.getScaledLogo(logoSize: Int, ancestor: Component): Icon? {
  val logo = logo ?: return null
  val scaleFactor = logoSize / INITIAL_LOGO_SIZE
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
