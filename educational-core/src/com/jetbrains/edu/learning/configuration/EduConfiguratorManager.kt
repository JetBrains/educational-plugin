package com.jetbrains.edu.learning.configuration

import com.intellij.lang.Language
import com.intellij.openapi.extensions.Extensions
import com.jetbrains.edu.learning.EduNames

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language, courseType and environment
   */
  @JvmStatic
  fun findConfigurator(courseType: String, environment: String, language: Language): EduConfigurator<out Any>? =
    allExtensions().find { extension -> extension.language == language.id &&
                                        extension.courseType == courseType &&
                                        extension.environment == environment}?.instance

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  @JvmStatic
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<out Any>>> =
    Extensions.getExtensions<EducationalExtensionPoint<EduConfigurator<out Any>>>(EduConfigurator.EP_NAME, null)
      .filter { it.instance.isEnabled }

  /**
   * Returns all languages with enabled [EduConfigurator] for [EduNames.PYCHARM] course type
   */
  @JvmStatic
  val supportedEduLanguages: List<String> by lazy {
    allExtensions().filter { it.courseType == EduNames.PYCHARM }.map { it.language }.toList()
  }
}
