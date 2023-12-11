package com.jetbrains.edu.learning.command

import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import org.apache.commons.cli.*
import kotlin.system.exitProcess

@Suppress("UnstableApiUsage")
abstract class EduAppStarterBase : ModernApplicationStarter() {

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
      saveAndExit(result.exitCode)
    }
    catch (e: Throwable) {
      LOG.error(e)
      exitProcess(1)
    }
  }

  protected abstract suspend fun doMain(course: Course, args: Args): CommandResult

  private fun parseArgs(args: List<String>): Args {
    val options = Options()

    val group = OptionGroup()
    group.isRequired = true
    group.addOption(Option(null, COURSE_ARCHIVE_PATH_OPTION, true, "Path to course archive file"))
    group.addOption(
      Option(null, MARKETPLACE_COURSE_LINK_OPTION, true, """Marketplace course link. Supported formats:
      - %course-id%
      - %course-id%-%plugin-name%
      - https://plugins.jetbrains.com/plugin/%course-id%
      - https://plugins.jetbrains.com/plugin/%course-id%-%plugin-name%.
      
      So, for https://plugins.jetbrains.com/plugin/16630-introduction-to-python course, you can pass:
      - 16630
      - 16630-introduction-to-python
      - https://plugins.jetbrains.com/plugin/16630
      - https://plugins.jetbrains.com/plugin/16630-introduction-to-python
    """.trimIndent())
    )
    options.addOptionGroup(group)
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

    val positionalArgs = cmd.argList
    if (positionalArgs.isEmpty()) {
      printHelp(options)
      logErrorAndExit("Path to project is missing")
    }

    return Args(positionalArgs.first(), cmd)
  }

  protected open fun addCustomArgs(options: Options) {}

  private fun printHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.width = 140
    @Suppress("DEPRECATION")
    formatter.printHelp("$commandName /path/to/project", options)
  }

  private fun loadCourse(args: Args): Course {
    val courseArchivePath = args.getOptionValue(COURSE_ARCHIVE_PATH_OPTION)
    val marketplaceCourseLink = args.getOptionValue(MARKETPLACE_COURSE_LINK_OPTION)
    return when {
      courseArchivePath != null -> {
        val course = EduUtilsKt.getLocalCourse(courseArchivePath)
        if (course == null) {
          logErrorAndExit("Failed to create course object from `$courseArchivePath` archive")
        }
        course
      }
      marketplaceCourseLink != null -> {
        val course = MarketplaceConnector.getInstance().getCourseInfoByLink(marketplaceCourseLink, searchPrivate = true)
        if (course == null) {
          logErrorAndExit("Failed to load Marketplace course `$marketplaceCourseLink`")
        }
        course
      }
      else -> error("unreachable")
    }
  }
  
  companion object {
    @JvmStatic
    protected val LOG = logger<EduCourseCreatorAppStarter>()
    
    private const val COURSE_ARCHIVE_PATH_OPTION = "archive"
    private const val MARKETPLACE_COURSE_LINK_OPTION = "marketplace"

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

class Args(
  val projectPath: String,
  private val cmd: CommandLine
) {
  fun getOptionValue(option: String): String? = cmd.getOptionValue(option)
}