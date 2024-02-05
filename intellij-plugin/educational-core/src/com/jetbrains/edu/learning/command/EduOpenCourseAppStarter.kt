package com.jetbrains.edu.learning.command

import com.intellij.featureStatistics.fusCollectors.LifecycleUsageTriggerCollector
import com.intellij.ide.AppLifecycleListener
import com.intellij.idea.IdeStarter
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.blockingContext
import com.jetbrains.edu.learning.map
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.onError
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
    val parser = ArgParser.createDefault(COMMAND_NAME)

    val parsedArgs = parser.parseArgs(args).onError { error ->
      LOG.error(error)
      parser.printHelp()
      app.saveAndExit(1)
      return
    }

    invokeAppListeners(args, lifecyclePublisher, asyncCoroutineScope)

    parsedArgs.loadCourse().map { course ->
      val result = withContext(Dispatchers.EDT) {
        JoinCourseDialog(course).showAndGet()
      }
      if (!result) {
        app.saveAndExit(0)
      }
    }.onError { error ->
      LOG.error(error)
      app.saveAndExit(1)
      return
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

  private suspend fun Application.saveAndExit(exitCode: Int) {
    blockingContext {
      exit(true, true, false, exitCode)
    }
  }

  companion object {
    private val LOG: Logger = logger<EduOpenCourseAppStarter>()

    private const val COMMAND_NAME = "openCourse"
  }
}
