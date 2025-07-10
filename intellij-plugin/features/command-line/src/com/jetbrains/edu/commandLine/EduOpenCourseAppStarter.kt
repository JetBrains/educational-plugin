package com.jetbrains.edu.commandLine

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.intellij.featureStatistics.fusCollectors.LifecycleUsageTriggerCollector
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.CliResult
import com.intellij.idea.IdeStarter
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.blockingContext
import com.intellij.platform.diagnostic.telemetry.impl.span
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.map
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.TOOLBOX
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adds `openCourse` command for IDE to open course dialog for given course
 *
 * Expected usages:
 * - `openCourse --marketplace %marketplace-course-link%`
 * - `openCourse --archive %/path/to/course/archive%`
 */
class EduOpenCourseAppStarter : IdeStarter() {

  override val isHeadless: Boolean
    get() = false

  override suspend fun openProjectIfNeeded(
    args: List<String>,
    app: Application,
    asyncCoroutineScope: CoroutineScope,
    lifecyclePublisher: AppLifecycleListener
  ) {
    val command = openCourseCommand()
    try {
      command.parse(args.drop(1))
    }
    catch (e: CliktError) {
      command.echoFormattedHelp(e)
      app.saveAndExit(1)
      return
    }

    invokeAppListeners(args, lifecyclePublisher, asyncCoroutineScope)
    val result = showOpenCourseDialog(command)
    when (result) {
      OpenCourseDialogResult.Ok -> Unit
      OpenCourseDialogResult.Canceled -> app.saveAndExit(0)
      is OpenCourseDialogResult.Error -> app.saveAndExit(1)
    }
  }

  // partially copied from IdeStarter because these listener calls are somehow in a user-overridable spot
  private suspend fun invokeAppListeners(
    args: List<String>,
    lifecyclePublisher: AppLifecycleListener,
    asyncCoroutineScope: CoroutineScope
  ) {
    span("app frame created callback") {
      runCatching {
        lifecyclePublisher.appFrameCreated(args)
      }.getOrLogException(thisLogger())
    }

    // must be after `AppLifecycleListener#appFrameCreated`, because some listeners can mutate the state of `RecentProjectsManager`
    asyncCoroutineScope.launch {
      LifecycleUsageTriggerCollector.onIdeStart()
    }
  }

  override fun canProcessExternalCommandLine(): Boolean = true

  @Suppress("MoveVariableDeclarationIntoWhen")
  override suspend fun processExternalCommandLine(args: List<String>, currentDirectory: String?): CliResult {
    val command = openCourseCommand()
    return try {
      command.parse(args.drop(1))

      val openCourseFound = command.focusOpenProject()
      if (openCourseFound) return CliResult.OK

      val dialogResult = showOpenCourseDialog(command)
      when (dialogResult) {
        OpenCourseDialogResult.Ok,
        OpenCourseDialogResult.Canceled -> CliResult.OK
        is OpenCourseDialogResult.Error -> CliResult(1, dialogResult.message)
      }
    }
    catch (e: CliktError) {
      CliResult(1, command.getFormattedHelp(e))
    }
  }

  /**
   * Looks for an already open course project mentioned in the command and focus its frame.
   *
   * Returns `true` if opened course was found and `false` otherwise
   */
  private fun EduOpenCourseCommand.focusOpenProject(): Boolean {
    val (project, course) = EduBuiltInServerUtils.focusOpenProject { course ->
      source.isCourseFromSource(course) && course.id.toString() == courseId
    } ?: return false

    CourseMetadataProcessor.applyProcessors(project, course, courseParams)

    return true
  }

  private fun openCourseCommand(): EduOpenCourseCommand {
    return EduOpenCourseCommand().apply {
      context {
        echoMessage = LOG.toMessageEchoer()
      }
    }
  }

  private suspend fun showOpenCourseDialog(command: EduOpenCourseCommand): OpenCourseDialogResult {
    return command.source.loadCourse(command.courseId).map { course ->
      val result = withContext(Dispatchers.EDT) {
        // If it's called from external command, the application frame can be not in focus,
        // but we want to show the corresponding dialog to a user
        requestFocus()
        JoinCourseDialog(course, downloadCourseContext = TOOLBOX, params = command.courseParams).showAndGet()
      }
      if (result) OpenCourseDialogResult.Ok else OpenCourseDialogResult.Canceled
    }.onError { error ->
      LOG.error(error)
      OpenCourseDialogResult.Error(error)
    }
  }

  private suspend fun Application.saveAndExit(exitCode: Int) {
    blockingContext {
      exit(true, true, false, exitCode)
    }
  }

  companion object {
    private val LOG: Logger = logger<EduOpenCourseAppStarter>()
  }

  private sealed interface OpenCourseDialogResult {
    data object Ok : OpenCourseDialogResult
    data object Canceled : OpenCourseDialogResult
    data class Error(val message: String) : OpenCourseDialogResult
  }
}

class EduOpenCourseCommand : EduCommand("openCourse") {

  val courseParams: Map<String, String> by option(
    "--course-params",
    help = "Additional parameters for a course project in JSON object format"
  )
    .convert { parseCourseParams(it) }
    .default(emptyMap())

  private fun parseCourseParams(value: String): Map<String, String> {
    return try {
      MAPPER.readValue(value)
    }
    catch (e: Exception) {
      throw IllegalArgumentException("JSON object expected, got `$value` instead", e)
    }
  }

  override suspend fun run() {}

  companion object {
    private val MAPPER = jsonMapper()
  }
}
