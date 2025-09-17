package com.jetbrains.edu.commandLine.validation

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.intellij.openapi.project.Project
import com.jetbrains.edu.commandLine.CommandResult
import com.jetbrains.edu.commandLine.EduAppStarterWrapper
import com.jetbrains.edu.commandLine.EduCourseProjectCommand
import com.jetbrains.edu.coursecreator.validation.CourseValidationHelper
import com.jetbrains.edu.coursecreator.validation.ValidationParams
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

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

  override val courseMode: CourseMode
    get() = CourseMode.EDUCATOR

  override suspend fun performProjectAction(project: Project, course: Course): CommandResult {
    val params = ValidationParams(validateTests, validateLinks)
    val result = CourseValidationHelper(params).validate(project, course)

    val outputConsumer = StdoutValidationOutputConsumer()
    TeamCityValidationResultConsumer(outputConsumer).consume(result)

    return if (result.isFailed) CommandResult.Error("Some tasks haven't finished successfully") else CommandResult.Ok
  }

  companion object {
    private const val VALIDATE_TESTS = "tests"
    private const val VALIDATE_LINKS = "links"

    const val VALIDATE_TEST_BY_DEFAULT = false
    const val VALIDATE_LINK_BY_DEFAULT = true
  }
}
