package com.jetbrains.edu.learning.compatibility

import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import kotlin.reflect.KClass

abstract class CourseCompatibilityProviderTestBase(private val clazz: KClass<out CourseCompatibilityProvider>) : EduTestCase() {

  fun `test course compatibility provider`() {
    val extensionPoints = CourseCompatibilityProviderEP.EP_NAME.extensions.filter { it.instance.javaClass == clazz.java }
    check(extensionPoints.isNotEmpty()) {
      "Cannot find extension point of `${clazz.qualifiedName}` class"
    }

    for (extensionPoint in extensionPoints) {
      val languageId = extensionPoint.language
      val courseType = extensionPoint.courseType
      val environment = extensionPoint.environment

      val lang = Language.findLanguageByID(extensionPoint.language) ?: error("Cannot find language with `$languageId` id")

      val compatibilityProvider = extensionPoint.instance
      val requiredPlugins = compatibilityProvider.requiredPlugins()
      val configurator = EduConfiguratorManager.findExtension(courseType, environment, lang)

      if (requiredPlugins != null) {
        check(configurator != null) { "Cannot find configurator for ${clazz.qualifiedName}" }

        for (info in requiredPlugins) {
          // BACKCOMPAT: 2019.3
          @Suppress("DEPRECATION")
          check(PluginManager.getPlugin(info.id) != null) { "Cannot find plugin with `${info.stringId}` id" }
        }
      }
      else {
        check(configurator == null) {
          """Unexpected `${configurator!!.javaClass.simpleName}` configurator for (languageId: "$languageId", environment: "$environment", courseType: "$courseType")"""
        }
      }
    }
  }
}
