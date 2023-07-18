package com.jetbrains.edu.learning.command

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import kotlin.system.exitProcess

/**
 * Adds `installCoursePlugins` command for IDE to install all necessary plugins for given course
 *
 * Expected usages:
 * - `installCoursePlugins %/path/to/project/dir% --marketplace %marketplace-course-link%`
 * - `installCoursePlugins %/path/to/project/dir% --archive %/path/to/course/archive%`
 */
class EduCoursePluginInstallerAppStarter : EduAppStarterBase() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "installCoursePlugins"

  override suspend fun doMain(course: Course, projectPath: String) {
    val provider = course.compatibilityProvider
    if (provider == null) {
      logErrorAndExit(course.incompatibleCourseMessage())
    }

    val pluginManager = PluginManager.getInstance()
    val allRequiredPlugins = provider.requiredPlugins().orEmpty() + course.pluginDependencies
    val pluginIds = allRequiredPlugins
      .map { PluginId.getId(it.stringId) }
      .filterTo(hashSetOf()) { pluginManager.findEnabledPlugin(it) == null }

    LOG.info("Installing: $pluginIds")

    @Suppress("UnstableApiUsage")
    ProgressManager.getInstance().run(object : InstallAndEnableTaskHeadlessImpl(pluginIds, {}) {
      override fun onThrowable(error: Throwable) {
        LOG.error("Failed to install plugins:", error)
        exitProcess(1)
      }
    })
  }
}
