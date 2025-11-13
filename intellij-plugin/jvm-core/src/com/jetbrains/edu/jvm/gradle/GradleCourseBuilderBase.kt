package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.WRAPPER
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.GRADLE_DIR_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract fun buildGradleTemplateName(course: Course): String
  open fun settingGradleTemplateName(course: Course): String = SETTINGS_FILE_NAME

  /**
   * Map from config file name which should be created in project to template file name
   */
  fun templates(course: Course): Map<String, String> {
    val templates = mutableMapOf<String, String>()
    if (!EduGradleUtils.hasCourseHaveGradleKtsFiles(course)) {
      val scriptTemplates = mapOf(
        DEFAULT_SCRIPT_NAME to buildGradleTemplateName(course),
        SETTINGS_FILE_NAME to settingGradleTemplateName(course),
      )
      /*
        Since 2025.10, build.gradle and settings.gradle are being put into the course archive.
        Courses created on an older version of the plugin may not have one of these files, and they must be generated.
        Since the templates have breaking changes in 2025.10.1:
        - For a student, if some files are missing, then a course was created before 2025.10.
          Then old templates must be generated to not break the Gradle. Old templates have "legacy-" prefix in their names.
        - For CC, new templates must be generated.
      */
      templates += if (course.isStudy) {
        scriptTemplates.applyLegacyPrefixToTemplateNames()
      }
      else {
        scriptTemplates
      }
    }
    if (course.isStudy) {
      // starting from release 2025.10.1, we put gradle-wrapper.properties in the archive.
      // For earlies courses, gradle-wrapper.properties should be created manually.
      // Because otherwise they could not be built due to incompatibility of build.gradle from the archive and the gradle version
      templates += GRADLE_WRAPPER_PROPERTIES_PATH to GRADLE_WRAPPER_PROPERTIES
    }
    return templates
  }

  open fun templateVariables(projectName: String): Map<String, Any> {
    return mapOf(
      PROJECT_NAME to GeneratorUtils.gradleSanitizeName(projectName),
      GRADLE_VERSION_VARIABLE to LEGACY_GRADLE_VERSION,
    ) + getKotlinTemplateVariables()
  }

  override fun refreshProject(project: Project, cause: RefreshCause) {
    GradleCourseRefresher.firstAvailable()?.refresh(project, cause)
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getDefaultSettings(): Result<JdkProjectSettings, String> = JdkProjectSettings.defaultSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  /*
   * Starting with 2025.10.1, we are starting to use new templates due to incompatibility with Gradle 9.0.0.
   * For these courses, we put in the archive both `settings.gradle` and `build.gradle`.
   * The problem is that some old courses (mainly Java courses) may have only one of those files in the archive,
   * which means we have to generate an old version of the other file for them.
   * Older versions of templates have a `legacy-` prefix.
   */
  private fun Map<String, String>.applyLegacyPrefixToTemplateNames(): Map<String, String> {
    return mapValues { (_, templateName) -> LEGACY_TEMPLATE_PREFIX + templateName }
  }

  companion object {
    @NonNls
    const val HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME: String = "hyperskill-settings.gradle"

    @VisibleForTesting
    const val LEGACY_TEMPLATE_PREFIX: String = "legacy-"

    val GRADLE_WRAPPER_PROPERTIES_PATH: String = GeneratorUtils.joinPaths(GRADLE_DIR_NAME, WRAPPER, GRADLE_WRAPPER_PROPERTIES)

    private const val GRADLE_VERSION_VARIABLE: String = "GRADLE_VERSION"
    // Starting from release 2025.10.1, we put gradle-wrapper.properties in the archive.
    // Earlier courses are considered to have gradle version 8.14.3
    private const val LEGACY_GRADLE_VERSION: String = "8.14.3"

    fun getKotlinTemplateVariables(): Map<String, Any> {
      return mapOf(
        // Uses the Kotlin version from stdlib the platform was compiled with, it should be enough for the course template.
        // toString() returns the "major.minor.patch" form.
        "KOTLIN_VERSION" to KotlinVersion.CURRENT.toString()
      )
    }
  }
}
