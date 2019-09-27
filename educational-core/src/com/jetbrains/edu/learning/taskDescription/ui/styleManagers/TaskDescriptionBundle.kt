package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.CommonBundle
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.annotations.PropertyKey
import java.util.*

internal object TaskDescriptionBundle {
  private const val BUNDLE_NAME = "style.browser"
  private val BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME)

  fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
    return CommonBundle.message(BUNDLE, key, *params)
  }

  fun getFloatParameter(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = message(
    if (SystemInfo.isMac) "mac.$key" else key).toFloat()

  fun getOsDependentParameter(key: String) = message(parameterNameWithOSPrefix(key))

  private fun parameterNameWithOSPrefix(name: String): String {
    return when {
      SystemInfo.isMac -> "mac.$name"
      SystemInfo.isWindows -> "win.$name"
      else -> "linux.$name"
    }
  }
}