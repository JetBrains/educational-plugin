package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  open val settingGradleTemplateName: String = SETTINGS_FILE_NAME

  /**
   * Map from config file name which should be created in project to template file name
   */
  open val templates: Map<String, String>
    get() = mapOf(DEFAULT_SCRIPT_NAME to buildGradleTemplateName,
                  SETTINGS_FILE_NAME to settingGradleTemplateName)

  open fun templateVariables(project: Project): Map<String, Any> {
    return mapOf(PROJECT_NAME to GeneratorUtils.sanitizeName(project.name))
  }

  override fun refreshProject(project: Project, cause: RefreshCause, listener: EduCourseBuilder.ProjectRefreshListener?) {
    // Gradle projects are refreshed by IDE itself on (re)opening
    if (cause == RefreshCause.STRUCTURE_MODIFIED || isUnitTestMode) {
      val refresher = GradleCourseRefresher.firstAvailable()
      if (refresher != null) {
        refresher.refresh(project, listener)
      }
      else {
        listener?.onFailure("Failed to find proper course refresher")
      }
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)
}
