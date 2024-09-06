package com.jetbrains.edu.learning.command

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
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists

/**
 * Loads given course, opens a project with loaded course and performs [performProjectAction]
 */
abstract class EduCourseProjectAppStarterBase : EduAppStarterBase<ArgsWithProjectPath>() {

  protected abstract val courseMode: CourseMode

  override fun createArgParser(): ArgParser<ArgsWithProjectPath> = ArgParser.createWithProjectPath(commandName)

  final override suspend fun doMain(course: Course, args: ArgsWithProjectPath): CommandResult {
    val configurator = course.configurator
    if (configurator == null) {
      return CommandResult.Error(course.incompatibleCourseMessage())
    }

    val courseBuilder = configurator.courseBuilder

    return when (val projectSettings = courseBuilder.getDefaultSettings()) {
      is Err -> CommandResult.Error(projectSettings.error)
      is Ok -> createCourseProject(course, projectSettings.value, args)
    }
  }

  private suspend fun createCourseProject(
    course: Course,
    projectSettings: EduProjectSettings,
    args: ArgsWithProjectPath
  ): CommandResult {
    cleanupCourseDir(args)

    EduThreadDumpService.getInstance().startThreadDumping()
    var errorMessage: String? = null

    val result = withAutoImportDisabled {
      val info = CourseCreationInfo(course, args.projectPath, projectSettings)
      val project = withContext(Dispatchers.EDT) {
        CoursesPlatformProvider.joinCourse(info, courseMode, null) {
          errorMessage = it.message?.message
        }
      }
      if (project != null) {
        Observation.awaitConfiguration(project)

        val result = performProjectAction(project, course, args)

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

  private fun cleanupCourseDir(args: ArgsWithProjectPath) {
    val courseDir = Paths.get(args.projectPath)
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

  protected abstract suspend fun performProjectAction(project: Project, course: Course, args: Args): CommandResult
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
