package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

class CourseCompatibilityProviderEP : BaseKeyedLazyInstance<CourseCompatibilityProvider>() {

  @Attribute("implementationClass")
  @RequiredElement
  var implementationClass: String? = null

  @Attribute("language")
  @RequiredElement
  var language: String = ""

  @Attribute("environment")
  var environment: String = ""

  override fun getImplementationClassName(): String? = implementationClass

  companion object {
    @JvmStatic
    val EP_NAME: ExtensionPointName<CourseCompatibilityProviderEP> = ExtensionPointName.create("Educational.compatibilityProvider")

    @JvmStatic
    fun find(languageId: String, environment: String): CourseCompatibilityProvider? {
      return EP_NAME.extensions.find { it.language == languageId && it.environment == environment }?.instance
    }
  }
}
