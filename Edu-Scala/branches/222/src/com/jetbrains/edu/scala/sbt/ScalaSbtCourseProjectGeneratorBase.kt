package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class ScalaSbtCourseProjectGeneratorBase(
  builder: ScalaSbtCourseBuilder,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {
  override fun prepareToOpen(project: Project, module: Module) {
    invokeAndWaitIfNeeded {
      GeneratorUtils.removeModule(project, module)
    }
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
  }
}
