package com.jetbrains.edu.commandLine

import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.onError
import kotlin.system.exitProcess

@Suppress("UnstableApiUsage")
abstract class EduAppStarterBase<T : Args> : ModernApplicationStarter() {

  @Suppress("OVERRIDE_DEPRECATION")
  abstract override val commandName: String

  override suspend fun start(args: List<String>) {
    try {
      val parsedArgs = parseArgs(args)
      val course = loadCourse(parsedArgs)
      val result = doMain(course, parsedArgs)
      if (result is CommandResult.Error) {
        LOG.error(result.message, result.throwable)
      }
      ApplicationManagerEx.getApplicationEx().exit(true, true, result.exitCode)
    }
    catch (e: Throwable) {
      LOG.error(e)
      exitProcess(1)
    }
  }

  protected abstract fun createArgParser(): ArgParser<T>
  protected abstract suspend fun doMain(course: Course, args: T): CommandResult

  private fun parseArgs(args: List<String>): T {
    val parser = createArgParser()
    return when (val result = parser.parseArgs(args)) {
      is Ok -> result.value
      is Err -> {
        logErrorAndExit(result.error)
      }
    }
  }

  private suspend fun loadCourse(args: Args): Course {
    return args.loadCourse().onError { error ->
      logErrorAndExit(error)
    }
  }

  companion object {
    @JvmStatic
    protected val LOG = logger<EduCourseCreatorAppStarter>()

    @JvmStatic
    fun logErrorAndExit(message: String): Nothing {
      LOG.error(message)
      exitProcess(1)
    }

    @JvmStatic
    protected fun Course.incompatibleCourseMessage(): String {
      return buildString {
        append("""Can't open `${course.name}` course (type="${course.itemType}", language="${course.languageId}", """)
        if (!course.languageVersion.isNullOrEmpty()) {
          append("""language version="${course.languageVersion}", """)
        }
        append("""environment="${course.environment}") with current IDE setup""")
      }
    }
  }
}
