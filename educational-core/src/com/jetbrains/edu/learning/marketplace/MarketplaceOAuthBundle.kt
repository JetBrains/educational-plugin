package com.jetbrains.edu.learning.marketplace

import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object MarketplaceOAuthBundle {

  private const val BUNDLE = "marketplace.marketplace-oauth"
  private var ourBundle: ResourceBundle? = null

  private val bundle: ResourceBundle?
    get() {
      if (ourBundle == null) {
        ourBundle = ResourceBundle.getBundle(BUNDLE)
      }
      return ourBundle
    }

  fun valueOrDefault(@PropertyKey(resourceBundle = BUNDLE) key: String, defaultValue: String, vararg params: Any): String {
    return CommonBundle.messageOrDefault(bundle, key, defaultValue, *params)
  }
}
