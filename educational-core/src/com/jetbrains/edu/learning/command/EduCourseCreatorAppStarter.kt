package com.jetbrains.edu.learning.command

import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider

/**
 * Adds `createCourse` command for IDE to create a course project.
 *
 * Expected usages:
 * - `createCourse %/path/to/project/dir% --marketplace %marketplace-course-link%`
 * - `createCourse %/path/to/project/dir% --archive %/path/to/course/archive%`
 */
class EduCourseCreatorAppStarter : EduAppStarterBase() {

  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "createCourse"

  override fun doMain(course: Course, projectPath: String) {
    val configurator = course.configurator
    if (configurator == null) {
      logErrorAndExit(course.incompatibleCourseMessage())
    }

    val courseBuilder = configurator.courseBuilder

    when (val projectSettings = courseBuilder.getDefaultSettings()) {
      is Err -> logErrorAndExit(projectSettings.error)
      is Ok -> createCourseProject(course, projectPath, projectSettings.value)
    }
  }

  private fun createCourseProject(
    course: Course,
    location: String,
    projectSettings: EduProjectSettings
  ) {
    var errorMessage: String? = null
    val project = withAutoImportDisabled {
      val info = CourseCreationInfo(course, location, projectSettings)
      val project = CoursesPlatformProvider.joinCourse(info, CourseMode.STUDENT, null) {
        errorMessage = it.message?.message
      }
      if (project != null) {
        @Suppress("UnstableApiUsage")
        ProjectManagerEx.getInstanceEx().saveAndForceCloseProject(project)
      }
      project
    }
    if (project == null) {
      val message = buildString {
        append("Failed to create course project")
        if (!errorMessage.isNullOrEmpty()) {
          append(". $errorMessage")
        }
      }
      logErrorAndExit(message)
    }
  }
}

private fun <T> withAutoImportDisabled(action: () -> T): T {
  val registryValue = Registry.get("external.system.auto.import.disabled")
  val oldValue = registryValue.asBoolean()
  registryValue.setValue(true)
  return try {
    action()
  }
  finally {
    registryValue.setValue(oldValue)
  }
}
