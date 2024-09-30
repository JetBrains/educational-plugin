package com.jetbrains.edu.commandLine

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.intellij.ide.CliResult
import com.intellij.ide.plugins.HeadlessPluginsInstaller
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.isHeadlessEnvironment
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Adds `installCoursePlugins` command for IDE to install all necessary plugins for given course
 *
 * Expected usages:
 * - `installCoursePlugins [%/path/to/project/dir%] --marketplace %marketplace-course-link%`
 * - `installCoursePlugins [%/path/to/project/dir%] --archive %/path/to/course/archive%`
 *
 * Note, path to project directory is optional for the command itself.
 * It's not prohibited since in case of remote server script,
 * path to project directory is required by the script itself as first positional argument.
 * But the command doesn't use it
 */
class EduCoursePluginInstallerAppStarter : EduAppStarterWrapper(EduCoursePluginInstallerCommand()) {
  override fun canProcessExternalCommandLine(): Boolean = true

  override suspend fun start(args: List<String>) {
    if (!isHeadlessEnvironment) {
      command.logErrorAndExit(
        "`${command.commandName}` requires headless environment only. " +
        "Try adding `-Djava.awt.headless=true` to your command (commandline: ${args.joinToString(" ")})"
      )
    }
    super.start(args)
  }

  @Suppress("UnstableApiUsage")
  override suspend fun processExternalCommandLine(args: List<String>, currentDirectory: String?): CliResult {
    // Temporary workaround not to fail on external `installCoursePlugin` command invocation.
    // Should be replaced with proper implementation later (https://youtrack.jetbrains.com/issue/EDU-6692)
    return CliResult.OK
  }
}

class EduCoursePluginInstallerCommand : EduCommand("installCoursePlugins") {

  // Instructs the command that we have an optional parameter which we have to pass in some cases,
  // but actually, we don't need it
  @Suppress("unused")
  val projectPath: Path? by argument("path to course project directory").convert { Paths.get(it) }.optional()

  override suspend fun doRun(course: Course): CommandResult {
    val provider = course.compatibilityProvider
    if (provider == null) {
      return CommandResult.Error(course.incompatibleCourseMessage())
    }

    val pluginManager = PluginManager.getInstance()
    val allRequiredPlugins = provider.requiredPlugins().orEmpty() + course.pluginDependencies
    val pluginIds = allRequiredPlugins
      .map { PluginId.getId(it.stringId) }
      .filterTo(hashSetOf()) { pluginManager.findEnabledPlugin(it) == null }

    LOG.info("Installing: $pluginIds")

    val installedPlugins = HeadlessPluginsInstaller.installPlugins(pluginIds)
    if (installedPlugins.size != pluginIds.size) {
      return CommandResult.Error("Failed to install plugins for `${course.name}` course")
    }
    return CommandResult.Ok
  }
}
