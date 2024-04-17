package com.jetbrains.edu.learning.compatibility

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import org.junit.Test
import kotlin.reflect.KClass

abstract class CourseCompatibilityProviderTestBase(private val clazz: KClass<out CourseCompatibilityProvider>) : EduTestCase() {

  @Test
  fun `test course compatibility provider`() {
    val extensionPoints = CourseCompatibilityProviderEP.EP_NAME.extensions.filter { it.instance.javaClass == clazz.java }
    check(extensionPoints.isNotEmpty()) {
      "Cannot find extension point of `${clazz.qualifiedName}` class"
    }

    for (extensionPoint in extensionPoints) {
      val languageId = extensionPoint.language
      val environment = extensionPoint.environment

      if (Language.findLanguageByID(extensionPoint.language) == null) {
        error("Cannot find language with `$languageId` id")
      }

      val compatibilityProvider = extensionPoint.instance
      val requiredPlugins = compatibilityProvider.requiredPlugins()
      val configurators = EduConfiguratorManager.allExtensions()
        .filter { it.language == languageId && it.environment == environment }
        .map { it.instance }

      if (requiredPlugins != null) {
        check(configurators.isNotEmpty()) { "Cannot find configurator for ${clazz.qualifiedName}" }

        for (info in requiredPlugins) {
          check(PluginManagerCore.getPlugin(PluginId.getId(info.stringId)) != null) { "Cannot find plugin with `${info.stringId}` id" }
        }
      }
      else {
        check(configurators.isEmpty()) {
          """Unexpected `$configurators` configurators for (languageId: "$languageId", environment: "$environment")"""
        }
      }
    }
  }
}
