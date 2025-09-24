package com.jetbrains.edu.commandLine.validation

import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import com.intellij.openapi.project.Project
import com.jetbrains.edu.commandLine.CommandResult
import com.jetbrains.edu.commandLine.EduAppStarterWrapper
import com.jetbrains.edu.commandLine.EduCourseProjectCommand
import com.jetbrains.edu.coursecreator.validation.CourseValidationHelper
import com.jetbrains.edu.coursecreator.validation.ValidationParams
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Adds `validateCourse` command for IDE to validate given course and report potential errors
 *
 * Expected usages:
 * - `validateCourse %/path/to/project/dir% --marketplace %marketplace-course-link%`
 * - `validateCourse %/path/to/project/dir% --archive %/path/to/course/archive%`
 *
 * Currently, the command is supposed to be run on TeamCity,
 * so test messages use TeamCity [test message format](https://www.jetbrains.com/help/teamcity/service-messages.html#Reporting+Tests).
 */
class EduCourseValidatorAppStarter : EduAppStarterWrapper(EduValidateCourseCommand())

class EduValidateCourseCommand : EduCourseProjectCommand("validateCourse") {

  val validateTests: Boolean by option("--$VALIDATE_TESTS", help = "Enables/disables test validation")
    .boolean()
    .default(VALIDATE_TEST_BY_DEFAULT)

  val validateLinks: Boolean by option("--$VALIDATE_LINKS", help = "Enables/disables task description link validation")
    .boolean()
    .default(VALIDATE_LINK_BY_DEFAULT)

  val outputFormat: OutputFormat by option("--output-format", help = "Output format of validation report")
    .choice(OutputFormat.values().associateBy { it.name.lowercase() })
    .default(OutputFormat.TEAMCITY, OutputFormat.TEAMCITY.name.lowercase())

  val outputType: OutputType by mutuallyExclusiveOptions(
    option("--output", help = "Print output to a file").convert { OutputType.File(Paths.get(it)) },
    option("--stdout", help = "Print output to stdout").flag().convert { if (it) OutputType.Stdout else null }
  ).single().default(OutputType.Stdout)

  override val courseMode: CourseMode
    get() = CourseMode.EDUCATOR

  override suspend fun performProjectAction(project: Project, course: Course): CommandResult {
    val params = ValidationParams(validateTests, validateLinks)
    val result = CourseValidationHelper(params).validate(project, course)

    val outputConsumer = when (val outputType = outputType) {
      is OutputType.File -> FileValidationOutputConsumer(outputType.path)
      OutputType.Stdout -> StdoutValidationOutputConsumer()
    }

    val resultConsumer = when (outputFormat) {
      OutputFormat.JSON -> JsonValidationResultConsumer(outputConsumer)
      OutputFormat.TEAMCITY -> TeamCityValidationResultConsumer(outputConsumer)
    }

    outputConsumer.use {
      resultConsumer.consume(result)
    }

    return if (result.isFailed) CommandResult.Error("Some tasks haven't finished successfully") else CommandResult.Ok
  }

  companion object {
    private const val VALIDATE_TESTS = "tests"
    private const val VALIDATE_LINKS = "links"

    const val VALIDATE_TEST_BY_DEFAULT = false
    const val VALIDATE_LINK_BY_DEFAULT = true
  }

  sealed class OutputType {
    data object Stdout : OutputType()
    data class File(val path: Path) : OutputType()
  }

  enum class OutputFormat {
    JSON,
    TEAMCITY
  }
}
