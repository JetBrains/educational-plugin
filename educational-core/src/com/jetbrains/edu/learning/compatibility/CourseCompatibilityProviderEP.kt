package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute
import com.jetbrains.edu.learning.EduNames

class CourseCompatibilityProviderEP : AbstractExtensionPointBean() {

  @Attribute("implementationClass")
  @RequiredElement
  var implementationClass: String? = null

  @Attribute("language")
  @RequiredElement
  var language: String = ""

  @Attribute("environment")
  var environment: String = ""

  val instance: CourseCompatibilityProvider by lazy {
    instantiateClass<CourseCompatibilityProvider>(implementationClass!!, ApplicationManager.getApplication().picoContainer)
  }

  companion object {
    @JvmStatic
    val EP_NAME: ExtensionPointName<CourseCompatibilityProviderEP> = ExtensionPointName.create("Educational.compatibilityProvider")

    @JvmStatic
    fun find(languageId: String, environment: String): CourseCompatibilityProvider? {
      return EP_NAME.extensions.find { it.language == languageId && it.environment == environment }?.instance
    }
  }
}
