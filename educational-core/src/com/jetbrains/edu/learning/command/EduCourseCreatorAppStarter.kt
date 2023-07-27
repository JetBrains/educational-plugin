package com.jetbrains.edu.learning.command

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.command.EduAppStarterBase.Companion.logErrorAndExit
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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

  override suspend fun doMain(course: Course, projectPath: String) {
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

  private suspend fun createCourseProject(
    course: Course,
    location: String,
    projectSettings: EduProjectSettings
  ) {
    var errorMessage: String? = null

    val listener = ProjectConfigurationListener()
    ApplicationManager.getApplication()
      .messageBus
      .connect()
      .subscribe(CourseProjectGenerator.COURSE_PROJECT_CONFIGURATION, listener)

    val project = withAutoImportDisabled {
      val info = CourseCreationInfo(course, location, projectSettings)
      val project = withContext(Dispatchers.EDT) {
        CoursesPlatformProvider.joinCourse(info, CourseMode.STUDENT, null) {
          errorMessage = it.message?.message
        }
      }
      if (project != null) {
        listener.waitForProjectConfiguration()
        // Some technologies do some work at project opening in startup activities,
        // and they may not expect that project is closed so early (C++, for example).
        // So, let's try to wait for them
        waitForPostStartupActivities(project)
        withContext(Dispatchers.EDT) {
          @Suppress("UnstableApiUsage")
          ProjectManagerEx.getInstanceEx().saveAndForceCloseProject(project)
        }
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

  private suspend fun waitForPostStartupActivities(project: Project) {
    val startupManager = StartupManager.getInstance(project)
    waitUntil { startupManager.postStartupActivityPassed() }
  }
}

private class ProjectConfigurationListener : CourseProjectGenerator.CourseProjectConfigurationListener {

  @Volatile
  private var isProjectConfigured: Boolean = false

  override fun onCourseProjectConfigured(project: Project) {
    isProjectConfigured = true
  }

  suspend fun waitForProjectConfiguration() {
    val timeout = System.getProperty("edu.create.course.timeout")?.toLong() ?: DEFAULT_TIMEOUT
    val startTime = System.currentTimeMillis()
    // Wait until the course project is fully configured
    waitUntil { isProjectConfigured || (startTime - System.currentTimeMillis()) > timeout }
    if (!isProjectConfigured) {
      logErrorAndExit("Project creation took more than $timeout ms")
    }
  }

  companion object {
    private val DEFAULT_TIMEOUT: Long = TimeUnit.MINUTES.toMillis(5)
  }
}

private suspend fun waitUntil(condition: () -> Boolean) {
  while (!condition()) {
    delay(50)
  }
}

private suspend fun <T> withAutoImportDisabled(action: suspend () -> T): T {
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
