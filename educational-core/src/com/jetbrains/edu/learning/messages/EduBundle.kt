package com.jetbrains.edu.learning.messages

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*

abstract class EduBundle {
  private var bundle: Reference<ResourceBundle>? = null

  protected fun getBundle(baseName: String): ResourceBundle {
    var bundle = com.intellij.reference.SoftReference.dereference(bundle)
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(baseName)
      this.bundle = SoftReference(bundle)
    }
    return bundle ?: error("Failed to initialize resource bundle $baseName")
  }
}