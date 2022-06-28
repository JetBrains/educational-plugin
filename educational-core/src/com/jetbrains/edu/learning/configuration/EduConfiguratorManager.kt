package com.jetbrains.edu.learning.configuration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.StepikNames

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language, courseType and environment
   */
  @JvmStatic
  fun findConfigurator(courseType: String, environment: String, language: Language): EduConfigurator<*>? =
    when (courseType) {
      CODEFORCES_COURSE_TYPE -> CodeforcesLanguageProvider.getConfigurator(language.id)
      MARKETPLACE -> findExtension(EduNames.PYCHARM, environment, language)?.instance
      else -> findExtension(courseType, environment, language)?.instance
    }

  @JvmStatic
  fun findExtension(courseType: String, environment: String, language: Language): EducationalExtensionPoint<EduConfigurator<*>>? {
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
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<*>>> =
    EducationalExtensionPoint.EP_NAME.extensions.filter { it.instance.isEnabled }

  /**
   * Returns all languages with enabled [EduConfigurator] for [EduNames.PYCHARM] course type
   */
  @JvmStatic
  val supportedEduLanguages: List<String>
    get() {
      return allExtensions().filter { it.courseType == EduNames.PYCHARM }.map { it.language }
    }

  private val compatibleCourseTypes: List<String> = listOf(CourseraNames.COURSE_TYPE, StepikNames.STEPIK_TYPE, EduFormatNames.MARKETPLACE)

  private fun compatibleCourseType(extension: EducationalExtensionPoint<EduConfigurator<*>>, courseType: String): Boolean {
    return extension.courseType == EduNames.PYCHARM && courseType in compatibleCourseTypes
  }

  @JvmStatic
  fun supportedEnvironments(language: Language): List<String> =
    allExtensions().filter { it.language == language.id && it.courseType == EduNames.PYCHARM }.map { it.environment }.distinct()
}
