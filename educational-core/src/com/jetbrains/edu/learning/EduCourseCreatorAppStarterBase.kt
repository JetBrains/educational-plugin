package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import org.apache.commons.cli.*
import kotlin.system.exitProcess

abstract class EduCourseCreatorAppStarterBase : ApplicationStarter {

  override fun main(args: List<String>) {
    val cmd = parseArgs(args)

    val projectPath = cmd.getOptionValue(PROJECT_PATH_OPTION)
    val courseArchivePath = cmd.getOptionValue(COURSE_ARCHIVE_PATH_OPTION)
    val marketplaceCourseLink = cmd.getOptionValue(MARKETPLACE_COURSE_LINK_OPTION)

    val course = when {
      courseArchivePath != null -> {
        val course = EduUtilsKt.getLocalCourse(courseArchivePath)
        if (course == null) {
          logErrorAndExit("Failed to create course object from `$courseArchivePath` archive")
        }
        course
      }
      marketplaceCourseLink != null -> {
        val course = MarketplaceConnector.getInstance().getCourseInfoByLink(marketplaceCourseLink)
        if (course == null) {
          logErrorAndExit("Failed to load Marketplace course `$marketplaceCourseLink`")
        }
        course
      }
      else -> error("unreachable")
    }

    val configurator = course.configurator
    if (configurator == null) {
      logErrorAndExit("""Can't open `${course.name}` course (type="${course.itemType}", language="${course.programmingLanguage}", environment="${course.environment}") with current IDE setup""")
    }

    val courseBuilder = configurator.courseBuilder

    when (val projectSettings = courseBuilder.getDefaultSettings()) {
      is Err -> logErrorAndExit(projectSettings.error)
      is Ok -> createCourseProject(course, projectPath, projectSettings.value)
    }

    ApplicationManagerEx.getApplicationEx().exit(true, true)
  }

  private fun createCourseProject(
    course: Course,
    location: String,
    projectSettings: EduProjectSettings
  ) {
    val info = CourseCreationInfo(course, location, projectSettings)

    var errorMessage: String? = null
    val project = CoursesPlatformProvider.joinCourse(info, CourseMode.STUDENT, null) {
      errorMessage = it.message?.message
    }
    if (project != null) {
      @Suppress("UnstableApiUsage")
      ProjectManagerEx.getInstanceEx().saveAndForceCloseProject(project)
    }
    else {
      val message = buildString {
        append("Failed to create course project")
        if (!errorMessage.isNullOrEmpty()) {
          append(". $errorMessage")
        }
      }
      logErrorAndExit(message)
    }
  }

  private fun parseArgs(args: List<String>): CommandLine {
    val options = Options()
    options.addRequiredOption(null, PROJECT_PATH_OPTION, true, "Path to course project location")

    val group = OptionGroup()
    group.isRequired = true
    group.addOption(Option(null, COURSE_ARCHIVE_PATH_OPTION, true, "Path to course archive file"))
    group.addOption(Option(null, MARKETPLACE_COURSE_LINK_OPTION, true, """Marketplace course link. Supported formats:
      - %course-id%
      - %course-id%-%plugin-name%
      - https://plugins.jetbrains.com/plugin/%course-id%
      - https://plugins.jetbrains.com/plugin/%course-id%-%plugin-name%.
      
      So, for https://plugins.jetbrains.com/plugin/16630-introduction-to-python course, you can pass:
      - 16630
      - 16630-introduction-to-python
      - https://plugins.jetbrains.com/plugin/16630
      - https://plugins.jetbrains.com/plugin/16630-introduction-to-python
    """.trimIndent()))
    options.addOptionGroup(group)

    val parser = DefaultParser()

    return try {
      parser.parse(options, args.drop(1).toTypedArray())
    }
    catch (e: ParseException) {
      LOG.error(e)
      val formatter = HelpFormatter()
      formatter.width = 140
      formatter.printHelp("createCourse", options)
      exitProcess(1)
    }
  }

  companion object {
    private val LOG = logger<EduCourseCreatorAppStarter>()

    private const val PROJECT_PATH_OPTION = "path"
    private const val COURSE_ARCHIVE_PATH_OPTION = "archive"
    private const val MARKETPLACE_COURSE_LINK_OPTION = "marketplace"

    private fun logErrorAndExit(message: String): Nothing {
      LOG.error(message)
      exitProcess(1)
    }
  }
}
