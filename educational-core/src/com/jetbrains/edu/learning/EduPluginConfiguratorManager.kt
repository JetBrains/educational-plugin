package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.Extensions

object EduPluginConfiguratorManager {

  /**
   * Returns any enabled [EduPluginConfigurator] for given language
   */
  @JvmStatic
  fun forLanguage(language: Language): EduPluginConfigurator<*>? =
          allExtensions().find { extension -> extension.key == language.id }?.instance

  /**
   * Returns all extension points of [EduPluginConfigurator] where instance of [EduPluginConfigurator] is enabled
   */
  @JvmStatic
  fun allExtensions(): List<LanguageExtensionPoint<EduPluginConfigurator<*>>> =
          Extensions.getExtensions<LanguageExtensionPoint<EduPluginConfigurator<*>>>(EduPluginConfigurator.EP_NAME, null)
                  .filter { it.instance.isEnabled }

}
