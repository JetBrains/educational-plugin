package com.jetbrains.edu.learning.command

import com.intellij.ide.CliResult
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.isHeadlessEnvironment

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
class EduCoursePluginInstallerAppStarter : EduAppStarterBase<Args>() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "installCoursePlugins"

  override fun createArgParser(): ArgParser<Args> = ArgParser.createDefault(commandName)

  override suspend fun start(args: List<String>) {
    if (!isHeadlessEnvironment) {
      logErrorAndExit("`$commandName` requires headless environment only. " +
                      "Try adding `-Djava.awt.headless=true` to your command (commandline: ${args.joinToString(" ")})")
    }
    super.start(args)
  }

  override suspend fun doMain(course: Course, args: Args): CommandResult {
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

    return installPlugins(pluginIds, course.name)
  }

  override fun canProcessExternalCommandLine(): Boolean = true

  override suspend fun processExternalCommandLine(args: List<String>, currentDirectory: String?): CliResult {
    // Temporary workaround not to fail on external `installCoursePlugin` command invocation.
    // Should be replaced with proper implementation later (https://youtrack.jetbrains.com/issue/EDU-6692)
    return CliResult.OK
  }
}
