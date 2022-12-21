package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : GradleCourseProjectGeneratorBase(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    val jdk = projectSettings.setUpProjectJdk(project, course, ::getJdk)
    setupGradleSettings(project, jdk)
    super.afterProjectGenerated(project, projectSettings)
  }

  protected open fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!)
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    val gradleCourseBuilder = courseBuilder as GradleCourseBuilderBase
    if (EduGradleUtils.hasCourseHaveGradleKtsFiles(course)) {
      return
    }
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
    // BACKCOMPAT: 2022.2. Make it private
    const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
