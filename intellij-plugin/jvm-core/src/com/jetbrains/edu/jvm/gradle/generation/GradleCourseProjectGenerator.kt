package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.GRADLE_WRAPPER_PROPERTIES_PATH
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: JdkProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    val jdk = projectSettings.setUpProjectJdk(project, course, ::getJdk)
    setupGradleSettings(project, jdk)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  protected open fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!)
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val gradleCourseBuilder = courseBuilder as GradleCourseBuilderBase
    return EduGradleUtils.createProjectGradleFiles(
      holder,
      gradleCourseBuilder.templates(holder.course),
      gradleCourseBuilder.templateVariables(holder.courseDir.name)
    )
  }

  override fun autoCreatedLaterAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    return if (!holder.course.isStudy) {
      listOf(EduFile(GRADLE_WRAPPER_PROPERTIES_PATH, TextualContents.EMPTY))
    }
    else emptyList()
  }

  protected open fun getJdk(settings: JdkProjectSettings): Sdk? {
    return settings.jdk
  }

  override suspend fun prepareToOpen(project: Project, module: Module) {
    super.prepareToOpen(project, module)
    @Suppress("UnstableApiUsage")
    writeAction { GeneratorUtils.removeModule(project, module) }
    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
  }

  companion object {
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
