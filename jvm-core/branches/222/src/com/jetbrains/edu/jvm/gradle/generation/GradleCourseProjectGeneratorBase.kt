package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.ui.EdtInvocationManager.invokeAndWaitIfNeeded
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class GradleCourseProjectGeneratorBase(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun prepareToOpen(project: Project, module: Module) {
    invokeAndWaitIfNeeded {
      GeneratorUtils.removeModule(project, module)
    }
    PropertiesComponent.getInstance(project).setValue(GradleCourseProjectGenerator.SHOW_UNLINKED_GRADLE_POPUP, false, true)
  }
}
