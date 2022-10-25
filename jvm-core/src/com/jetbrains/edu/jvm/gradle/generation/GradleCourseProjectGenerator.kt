package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun projectOpened(project: Project, module: Module) {
    invokeAndWaitIfNeeded {
      GeneratorUtils.removeModule(project, module)
    }

    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    val jdk = projectSettings.setUpProjectJdk(project, ::getJdk)
    setupGradleSettings(project, jdk)
    super.afterProjectGenerated(project, projectSettings)
  }

  protected open fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!)
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    val gradleCourseBuilder = courseBuilder as GradleCourseBuilderBase
    EduGradleUtils.createProjectGradleFiles(holder,
                                            gradleCourseBuilder.templates,
                                            gradleCourseBuilder.templateVariables(holder.courseDir.name))
  }

  protected open fun getJdk(settings: JdkProjectSettings): Sdk? {
    return settings.jdk
  }

  companion object {

    // Unfortunately, org.jetbrains.plugins.gradle.service.project.GradleStartupActivity#SHOW_UNLINKED_GRADLE_POPUP is private
    // so create own const
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
