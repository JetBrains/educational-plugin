package com.jetbrains.edu.learning.messages

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.reference.SoftReference
import java.lang.ref.Reference
import java.util.*

/**
 * Inherit this class for properties like sizes or OAuth keys.
 * It doesn't support params, default values or replacements.
 * For user visible error messages use [EduBundle].
 */
abstract class EduPropertiesBundle(private val bundleName: String) {

  private var ourBundle: Reference<ResourceBundle?>? = null

  private val bundle: ResourceBundle
    get() {
      var bundle = SoftReference.dereference(ourBundle)
      if (bundle == null) {
        bundle = ResourceBundle.getBundle(bundleName)
        ourBundle = java.lang.ref.SoftReference(bundle)
      }
      return bundle!!
    }

  protected fun valueOrEmpty(key: String): String {
    return try {
      bundle.getString(key)
    }
    catch (e: MissingResourceException) {
      LOG.error(e)
      ""
    }
  }

  companion object {
    private val LOG: Logger = logger<EduPropertiesBundle>()
  }
}