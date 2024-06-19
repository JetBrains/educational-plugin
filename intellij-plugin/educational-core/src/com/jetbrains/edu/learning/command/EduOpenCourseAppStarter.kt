package com.jetbrains.edu.learning.command

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
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.map
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.TOOLBOX
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
    val parsedArgs = parseArgs(args).onError {
      app.saveAndExit(1)
      return
    }

    invokeAppListeners(args, lifecyclePublisher, asyncCoroutineScope)

    val result = showOpenCourseDialog(parsedArgs)
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
    val parsedArgs = parseArgs(args).onError {
      return CliResult(1, it)
    }
    val dialogResult = showOpenCourseDialog(parsedArgs)
    return when (dialogResult) {
      OpenCourseDialogResult.Ok,
      OpenCourseDialogResult.Canceled -> CliResult.OK
      is OpenCourseDialogResult.Error -> CliResult(1, dialogResult.message)
    }
  }

  private fun parseArgs(args: List<String>): Result<Args, String> {
    val parser = ArgParser.createDefault(COMMAND_NAME)
    val result = parser.parseArgs(args)
    if (result is Err) {
      LOG.error(result.error)
    }
    return result
  }

  private suspend fun showOpenCourseDialog(args: Args): OpenCourseDialogResult {
    return args.loadCourse().map { course ->
      val result = withContext(Dispatchers.EDT) {
        // If it's called from external command, the application frame can be not in focus,
        // but we want to show the corresponding dialog to a user
        requestFocus()
        JoinCourseDialog(course, downloadCourseContext = TOOLBOX).showAndGet()
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

    private const val COMMAND_NAME = "openCourse"
  }

  private sealed interface OpenCourseDialogResult {
    data object Ok : OpenCourseDialogResult
    data object Canceled : OpenCourseDialogResult
    data class Error(val message: String) : OpenCourseDialogResult
  }
}
