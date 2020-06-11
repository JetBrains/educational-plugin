package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import java.awt.Component
import javax.swing.Icon
import kotlin.math.max

private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.newproject.ui.utils")
private const val INITIAL_LOGO_SIZE = 16f

val Course.logo: Icon?
  get() {
    val logo = configurator?.logo ?: compatibilityProvider?.logo
    if (logo == null) {
      val language = languageDisplayName
      LOG.info("configurator and compatibilityProvider are null. language: $language, course type: $itemType, environment: $environment")
    }

    return logo
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

fun browseHyperlink(message: ValidationMessage?) {
  if (message == null) {
    return
  }
  val hyperlink = message.hyperlinkAddress
  if (hyperlink != null) {
    BrowserUtil.browse(hyperlink)
  }
}

fun Icon.to24() = IconUtil.scale(this, null, JBUIScale.scale(24f / max(iconHeight, iconWidth)))
