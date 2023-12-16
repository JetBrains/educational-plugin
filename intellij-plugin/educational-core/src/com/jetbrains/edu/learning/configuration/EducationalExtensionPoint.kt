package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.jetbrains.edu.learning.courseFormat.EduFormatNames

class EducationalExtensionPoint<T> : BaseKeyedLazyInstance<T>() {
  @Attribute("implementationClass")
  @RequiredElement
  var implementationClass: String? = null

  @Attribute("language")
  @RequiredElement
  var language = ""

  @Attribute("courseType")
  var courseType = EduFormatNames.PYCHARM

  @Attribute("environment")
  var environment = ""

  @Attribute("displayName")
  var displayName: String? = null

  override fun getImplementationClassName(): String? = implementationClass

  companion object {
    val EP_NAME: ExtensionPointName<EducationalExtensionPoint<EduConfigurator<*>>> = ExtensionPointName.create("Educational.configurator")
  }
}
