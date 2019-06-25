package com.jetbrains.edu.learning.configuration

import com.intellij.lang.Language
import com.intellij.openapi.extensions.Extensions
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.stepik.StepikNames

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language, courseType and environment
   */
  @JvmStatic
  fun findConfigurator(courseType: String, environment: String, language: Language): EduConfigurator<out Any>? =
    findExtension(courseType, environment, language)?.instance

  @JvmStatic
  fun findExtension(courseType: String, environment: String, language: Language): EducationalExtensionPoint<EduConfigurator<out Any>>? {
    var configurator =
      allExtensions().find { extension ->
        extension.language == language.id &&
        extension.courseType == courseType &&
        extension.environment == environment
      }
    if (configurator == null) {
      configurator = allExtensions().find { extension ->
        extension.language == language.id &&
        compatibleCourseType(extension, courseType) &&
        extension.environment == environment
      }
    }
    return configurator
  }

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  @JvmStatic
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<out Any>>> =
    Extensions.getExtensions<EducationalExtensionPoint<EduConfigurator<out Any>>>(EducationalExtensionPoint.EP_NAME, null)
      .filter { it.instance.isEnabled }

  /**
   * Returns all languages with enabled [EduConfigurator] for [EduNames.PYCHARM] course type
   */
  @JvmStatic
  val supportedEduLanguages: List<String> by lazy {
    allExtensions().filter { it.courseType == EduNames.PYCHARM }.map { it.language }
  }

  private fun compatibleCourseType(extension: EducationalExtensionPoint<EduConfigurator<out Any>>, courseType: String): Boolean {
    return (courseType == CourseraNames.COURSE_TYPE || courseType == StepikNames.STEPIK_TYPE) && extension.courseType == EduNames.PYCHARM
  }

  @JvmStatic
  fun supportedEnvironments(language: Language): List<String> =
    allExtensions().filter { it.language == language.id && it.courseType == EduNames.PYCHARM }.map { it.environment }.distinct()
}
