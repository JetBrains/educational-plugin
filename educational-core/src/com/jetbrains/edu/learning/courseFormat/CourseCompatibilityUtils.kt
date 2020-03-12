package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager.supportedEduLanguages
import com.jetbrains.edu.learning.stepik.StepikNames

object CourseCompatibilityUtils {

  private val LOG: Logger = Logger.getInstance(CourseCompatibilityUtils::class.java)

  @JvmStatic
  fun isCourseCompatibility(courseInfo: EduCourse): CourseCompatibility {
    val supportedLanguages = supportedEduLanguages

    val courseFormat: String = courseInfo.type
    val typeLanguage = StringUtil.split(courseFormat, " ")
    if (typeLanguage.size < 2) {
      return CourseCompatibility.Unsupported
    }
    val prefix = typeLanguage[0]

    if (!supportedLanguages.contains(courseInfo.languageID)) return CourseCompatibility.Unsupported
    if (!prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
      return CourseCompatibility.Unsupported
    }
    val versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length)
    if (versionString.isEmpty()) return CourseCompatibility.Compatible
    return try {
      val version = Integer.valueOf(versionString)
      if (version <= JSON_FORMAT_VERSION) {
        CourseCompatibility.Compatible
      }
      else {
        CourseCompatibility.IncompatibleVersion
      }
    }
    catch (e: NumberFormatException) {
      LOG.info("Wrong version format", e)
      CourseCompatibility.Unsupported
    }
  }
}
