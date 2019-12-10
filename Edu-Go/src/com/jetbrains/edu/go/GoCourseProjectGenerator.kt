package com.jetbrains.edu.go

import com.goide.sdk.GoSdkService
import com.goide.vgo.configuration.VgoProjectSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseProjectGenerator(builder: GoCourseBuilder, course: Course) :
  CourseProjectGenerator<GoProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: GoProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    GoSdkService.getInstance(project).setSdk(projectSettings.sdk)
    VgoProjectSettings.getInstance(project).isIntegrationEnabled = true
  }
}
