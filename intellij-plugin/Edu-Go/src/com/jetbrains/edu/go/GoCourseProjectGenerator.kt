package com.jetbrains.edu.go

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdkService
import com.goide.vgo.configuration.VgoProjectSettings
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseProjectGenerator(builder: GoCourseBuilder, course: Course) :
  CourseProjectGenerator<GoProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: GoProjectSettings, onConfigurationFinished: () -> Unit) {
    GoSdkService.getInstance(project).setSdk(projectSettings.sdk)
    val module = ModuleManager.getInstance(project).modules.singleOrNull()
    if (module != null) {
      // Enable go support in created course project.
      // It will be enabled anyway by `GoPluginInitialConfigurator`.
      // But because `GoPluginInitialConfigurator` is registered `backgroundPostStartupActivity`,
      // the support will be enabled with some delay.
      // As a result, our checker tests don't work as expected because they are run before `GoPluginInitialConfigurator#runActivity`.
      // So let's enable it by default - we're absolutely sure that it's Go project
      GoModuleSettings.getInstance(module).isGoSupportEnabled = true
    }
    VgoProjectSettings.getInstance(project).isIntegrationEnabled = true
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }
}
