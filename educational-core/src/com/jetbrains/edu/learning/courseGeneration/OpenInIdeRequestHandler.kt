package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.pluginCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.getRequiredPluginsMessage

abstract class OpenInIdeRequestHandler<in T : OpenInIdeRequest> {
  @Suppress("UnstableApiUsage")
  @get:DialogTitle
  abstract val courseLoadingProcessTitle: String

  abstract fun openInExistingProject(request: T, findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Boolean

  abstract fun getCourse(request: T, indicator: ProgressIndicator): Result<Course, String>

  protected fun Course.validateLanguage(projectLanguage: String = course.programmingLanguage): Result<Unit, String> {
    val pluginCompatibility = pluginCompatibility()
    if (pluginCompatibility is CourseCompatibility.PluginsRequired) {
      val requiredPluginsMessage = getRequiredPluginsMessage(pluginCompatibility.toInstallOrEnable)
      val helpLink = "https://www.jetbrains.com/help/idea/managing-plugins.html"
      return Err(
        """$requiredPluginsMessage<a href="$helpLink">${EduCoreBundle.message("course.dialog.error.plugin.install.and.enable")}.</a>"""
      )
    }

    if (configurator == null) {
      return Err(EduCoreBundle.message("rest.service.language.not.supported",
                                       ApplicationNamesInfo.getInstance().productName,
                                       projectLanguage.capitalize()))
    }
    return Ok(Unit)
  }
}