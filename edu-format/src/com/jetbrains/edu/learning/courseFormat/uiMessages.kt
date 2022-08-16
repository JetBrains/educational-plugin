package com.jetbrains.edu.learning.courseFormat

import org.jetbrains.annotations.PropertyKey
import java.lang.reflect.Method
import java.text.MessageFormat
import java.util.*

const val FORMAT_BUNDLE = "messages.EduFormatBundle"
private const val BUNDLE_CLASS = "com.jetbrains.edu.learning.messages.EduFormatBundle"
private const val INSTANCE = "INSTANCE"
private val LOG = logger<StudyItem>()

val bundleClass: Class<*>? = try {
  Class.forName(BUNDLE_CLASS)
}
catch (e: Throwable) {
  null
}
val messageMethod: Method? = try {
  bundleClass?.getMethod("message", String::class.java, Array<Any>::class.java)
}
catch (e: Throwable) {
  null
}

internal fun message(@PropertyKey(resourceBundle = FORMAT_BUNDLE) key: String, vararg params: Any): String {
  return if (bundleClass == null || messageMethod == null) {
    return bundledMessage(key, params)
  }
  else {
    val bundleObject = bundleClass.getDeclaredField(INSTANCE)
    try {
      messageMethod.invoke(bundleObject, key, params) as String
    }
    catch (e: Throwable) {
      LOG.warn("Failed to invoke `$BUNDLE_CLASS.message()`", e)
      bundledMessage(key, params)
    }
  }
}

private fun bundledMessage(key: String, vararg params: Any): String {
  val resourceBundle = ResourceBundle.getBundle(FORMAT_BUNDLE)
  val value = resourceBundle.getString(key)
  return postprocessValue(value, params)
}

private fun postprocessValue(value: String, vararg params: Any): String {
  if (params.isNotEmpty() && value.indexOf('{') >= 0) {
    return try {
      MessageFormat(value).format(params)
    }
    catch (e: IllegalArgumentException) {
      "!invalid format: `$value`!"
    }
  }
  return value
}
