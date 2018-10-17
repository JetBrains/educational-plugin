package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.openapi.extensions.Extensions

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language
   */
  @JvmStatic
  fun forLanguage(language: Language, courseType: String): EduConfigurator<out Any>? =
          allExtensions().find { extension -> extension.language == language.id &&
                                 extension.courseType == courseType}?.instance

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  @JvmStatic
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<out Any>>> =
    Extensions.getExtensions<EducationalExtensionPoint<EduConfigurator<out Any>>>(EduConfigurator.EP_NAME, null)
      .filter { it.instance.isEnabled }

}
