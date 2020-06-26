package com.jetbrains.edu.go

import com.goide.sdk.GoSdkService
import com.goide.vgo.configuration.VgoProjectSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.util.*

class GoCourseProjectGenerator(builder: GoCourseBuilder, course: Course) :
  CourseProjectGenerator<GoProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: GoProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    GoSdkService.getInstance(project).setSdk(projectSettings.sdk)
    VgoProjectSettings.getInstance(project).isIntegrationEnabled = true
    fixLinebreaks()
  }

  // Without this, Go Plugin will use \r\n as separators on all systems
  private fun fixLinebreaks() {
    try {
      Registry.get(PTY).setValue(false)
    }
    catch (ignored: MissingResourceException) {
      LOG.warn("Key $PTY was removed from Go Support")
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GoCourseProjectGenerator::class.java)
    private const val PTY = "go.run.processes.with.pty"
  }
}
