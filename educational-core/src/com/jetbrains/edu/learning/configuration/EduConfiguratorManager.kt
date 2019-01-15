package com.jetbrains.edu.learning.configuration

import com.intellij.lang.Language
import com.intellij.openapi.extensions.Extensions

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language and courseType
   */
  @JvmStatic
  fun forLanguageAndCourseType(courseType: String, language: Language): EduConfigurator<out Any>? =
    allExtensions().find { extension -> extension.language == language.id &&
                                        extension.courseType == courseType}?.instance

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  @JvmStatic
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<out Any>>> =
    Extensions.getExtensions<EducationalExtensionPoint<EduConfigurator<out Any>>>(EduConfigurator.EP_NAME, null)
      .filter { it.instance.isEnabled }

  /**
   * Returns all languages with enabled [EduConfigurator]
   */
  @JvmStatic
  val supportedLanguages: List<String> by lazy {
    allExtensions().map{ it.language }.toList()
  }
}
