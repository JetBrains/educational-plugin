package com.jetbrains.edu.learning.courseFormat

import org.jetbrains.annotations.PropertyKey
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.text.MessageFormat
import java.util.*
import java.util.logging.Level

const val FORMAT_BUNDLE = "messages.EduFormatBundle"
private const val BUNDLE_CLASS = "com.jetbrains.edu.learning.messages.EduFormatBundle"
private const val INSTANCE = "INSTANCE"
private val LOG = logger<StudyItem>()

val messageMethod: MethodHandle? = findMessageMethod()

private fun findMessageMethod(): MethodHandle? {
  return try {
    val lookup = MethodHandles.lookup()
    val bundleClass = lookup.findClass(BUNDLE_CLASS)
    val methodType = MethodType.methodType(String::class.java, String::class.java, Array<Any>::class.java)
    val messageMethod = lookup.findVirtual(bundleClass, "message", methodType)
    val instanceGetter = lookup.findStaticGetter(bundleClass, INSTANCE, bundleClass)
    val bundleInstance = instanceGetter.invoke()
    messageMethod.bindTo(bundleInstance)
  }
  catch (e: Throwable) {
    null
  }
}

internal fun message(@PropertyKey(resourceBundle = FORMAT_BUNDLE) key: String, vararg params: Any): String {
  return if (messageMethod == null) {
    return bundledMessage(key, params)
  }
  else {
    try {
      messageMethod.invoke(key, params) as String
    }
    catch (e: Throwable) {
      LOG.log(Level.WARNING, "Failed to invoke `$BUNDLE_CLASS.message()`", e)
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
