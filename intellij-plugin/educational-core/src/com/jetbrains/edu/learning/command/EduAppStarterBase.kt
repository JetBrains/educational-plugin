package com.jetbrains.edu.learning.command

import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
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

  protected abstract suspend fun doMain(course: Course, args: T): CommandResult

  private fun parseArgs(args: List<String>): T {
    val options = Options()

    val courseSourceGroup = OptionGroup()
    courseSourceGroup.isRequired = true
    for (courseSource in CourseSource.values()) {
      courseSourceGroup.addOption(Option(null, courseSource.option, true, courseSource.description))
    }
    options.addOptionGroup(courseSourceGroup)

    addCustomArgs(options)

    val parser = DefaultParser()

    val cmd = try {
      parser.parse(options, args.drop(1).toTypedArray())
    }
    catch (e: ParseException) {
      printHelp(options)
      LOG.error(e)
      exitProcess(1)
    }

    return when (val result = createArgs(cmd)) {
      is Ok -> result.value
      is Err -> {
        printHelp(options)
        logErrorAndExit("Path to project is missing")
      }
    }
  }

  protected abstract fun createArgs(cmd: CommandLine): Result<T, String>
  protected open fun addCustomArgs(options: Options) {}

  private fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.width = 140
    @Suppress("DEPRECATION")
    formatter.printHelp("$commandName /path/to/project", options)
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

open class Args(private val cmd: CommandLine) {
  fun getOptionValue(option: String): String? = cmd.getOptionValue(option)
}
