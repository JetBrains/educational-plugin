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
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.gradle.util.GradleConstants.*

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  open val settingGradleTemplateName: String = SETTINGS_FILE_NAME

  open val buildGradleKtsTemplateName: String? = null
  open val settingGradleKtsTemplateName: String? = null

  val buildGradleTemplate: Pair<String, String>
    get() = DEFAULT_SCRIPT_NAME to buildGradleTemplateName

  val settingsGradleTemplate: Pair<String, String>
    get() = SETTINGS_FILE_NAME to settingGradleTemplateName

  val buildGradleKtsTemplate: Pair<String, String>?
    get() = buildGradleKtsTemplateName?.let { KOTLIN_DSL_SCRIPT_NAME to it }

  val settingsGradleKtsTemplate: Pair<String, String>?
    get() = settingGradleKtsTemplateName?.let { KOTLIN_DSL_SETTINGS_FILE_NAME to it }

  open fun templateVariables(projectName: String): Map<String, Any> {
    return mapOf(PROJECT_NAME to GeneratorUtils.gradleSanitizeName(projectName))
  }

  override fun refreshProject(project: Project, cause: RefreshCause) {
    GradleCourseRefresher.firstAvailable()?.refresh(project, cause)
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    @NonNls
    const val HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME: String = "hyperskill-settings.gradle"
  }
}
