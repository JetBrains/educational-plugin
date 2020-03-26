package com.jetbrains.edu.learning.messages

import com.intellij.CommonBundle
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*

// HACK: It will shadow realisation from education-core module
abstract class EduBundle(private val pathToBundle: String) {
  private var bundle: Reference<ResourceBundle>? = null

  private fun getBundle(baseName: String): ResourceBundle {
    var bundle = com.intellij.reference.SoftReference.dereference(bundle)
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(baseName)
      this.bundle = SoftReference(bundle)
    }
    return bundle ?: error("Failed to initialize resource bundle $baseName")
  }

  protected fun getMessage(key: String, vararg params: Any): String {
    return CommonBundle.message(getBundle(pathToBundle), key, *params)
  }
}