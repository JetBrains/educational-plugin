package com.jetbrains.edu.commandLine

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.backend.observation.Observation
import com.intellij.util.io.delete
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
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
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.pathString

abstract class EduCourseProjectCommand(name: String) : EduCommand(name) {
  val courseDir: Path by argument("Path to course project directory").convert { Paths.get(it) }

  protected abstract val courseMode: CourseMode

  override suspend fun doRun(course: Course): CommandResult {
    val configurator = course.configurator
    if (configurator == null) {
      return CommandResult.Error(course.incompatibleCourseMessage())
    }

    val courseBuilder = configurator.courseBuilder

    return when (val projectSettings = courseBuilder.getDefaultSettings()) {
      is Err -> CommandResult.Error(projectSettings.error)
      is Ok -> createCourseProject(course, projectSettings.value)
    }
  }

  private suspend fun createCourseProject(course: Course, projectSettings: EduProjectSettings): CommandResult {
    cleanupCourseDir()

    var errorMessage: String? = null

    val listener = ProjectConfigurationListener(this)
    ApplicationManager.getApplication()
      .messageBus
      .connect()
      .subscribe(CourseProjectGenerator.COURSE_PROJECT_CONFIGURATION, listener)

    val result = withAutoImportDisabled {
      val info = CourseCreationInfo(course, courseDir.pathString, projectSettings)
      val project = withContext(Dispatchers.EDT) {
        CoursesPlatformProvider.joinCourse(info, courseMode, null) {
          errorMessage = it.message?.message
        }
      }
      if (project != null) {
        listener.waitForProjectConfiguration()
        // Some technologies do some work at project opening in startup activities,
        // and they may not expect that project is closed so early (C++, for example).
        // So, let's try to wait for them
        Observation.awaitConfiguration(project)

        val result = performProjectAction(project, course)

        withContext(Dispatchers.EDT) {
          @Suppress("UnstableApiUsage")
          ProjectManagerEx.getInstanceEx().saveAndForceCloseProject(project)
        }
        result
      }
      else {
        val message = buildString {
          append("Failed to create course project")
          if (!errorMessage.isNullOrEmpty()) {
            append(". $errorMessage")
          }
        }

        CommandResult.Error(message)
      }
    }

    return result
  }

  private fun cleanupCourseDir() {
    if (courseDir.exists()) {
      // Do not try to delete the directory itself since it may have different permissions
      val children = mutableListOf<Path>()
      // Replace with Path#walkFileTree when it becomes stable
      Files.walkFileTree(courseDir, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
          return if (dir == courseDir) {
            FileVisitResult.CONTINUE
          }
          else {
            children.add(dir)
            FileVisitResult.SKIP_SUBTREE
          }
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
          children.add(file)
          return FileVisitResult.CONTINUE
        }
      })

      children.forEach { it.delete() }
    }
  }

  protected abstract suspend fun performProjectAction(project: Project, course: Course): CommandResult
}

private class ProjectConfigurationListener(
  private val command: EduCommand
) : CourseProjectGenerator.CourseProjectConfigurationListener {

  @Volatile
  private var isProjectConfigured: Boolean = false

  override fun onCourseProjectConfigured(project: Project) {
    isProjectConfigured = true
  }

  // BACKCOMPAT: 2023.2. Consider using `ActivityTracker` instead.
  // See https://youtrack.jetbrains.com/issue/IJPL-170/Provide-API-for-tracking-configuration-activities-in-IDE
  suspend fun waitForProjectConfiguration() {
    val timeout = System.getProperty("edu.create.course.timeout")?.toLong() ?: DEFAULT_TIMEOUT
    val startTime = System.currentTimeMillis()
    // Wait until the course project is fully configured
    waitUntil { isProjectConfigured || (startTime - System.currentTimeMillis()) > timeout }
    if (!isProjectConfigured) {
      command.logErrorAndExit("Project creation took more than $timeout ms")
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
