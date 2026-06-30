package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkLanguageEnvironment>(builder, course) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val gradleCourseBuilder = courseBuilder as GradleCourseBuilderBase
    return EduGradleUtils.createProjectGradleFiles(
      holder,
      gradleCourseBuilder.templates(holder.course),
      gradleCourseBuilder.templateVariables(holder.courseDir.name, holder)
    )
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
