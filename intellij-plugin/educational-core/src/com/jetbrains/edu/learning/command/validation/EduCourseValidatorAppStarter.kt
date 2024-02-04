package com.jetbrains.edu.learning.command.validation

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.command.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.apache.commons.cli.Option

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
class EduCourseValidatorAppStarter : EduCourseProjectAppStarterBase() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "validateCourse"

  override val courseMode: CourseMode
    get() = CourseMode.EDUCATOR

  override fun createArgParser(): ArgParser<ArgsWithProjectPath> {
    return super.createArgParser().withCustomArgs { options ->
      options.addOption(
        Option(null, VALIDATE_TESTS, true, "Enables/disables test validation. `$VALIDATE_TEST_BY_DEFAULT` by default")
      ).addOption(
        Option(null, VALIDATE_LINKS, true, "Enables/disables task description link validation. `$VALIDATE_LINK_BY_DEFAULT` by default")
      )
    }
  }

  override suspend fun performProjectAction(project: Project, course: Course, args: Args): CommandResult {
    val validateTests = args.getOptionValue(VALIDATE_TESTS)?.toBoolean() ?: VALIDATE_TEST_BY_DEFAULT
    val validateLinks = args.getOptionValue(VALIDATE_LINKS)?.toBoolean() ?: VALIDATE_LINK_BY_DEFAULT
    val params = ValidationParams(validateTests, validateLinks)

    val result = CourseValidationHelper(params, StdoutServiceMessageConsumer).validate(project, course)

    return if (result) CommandResult.Ok else CommandResult.Error("Some tasks haven't finished successfully")
  }

  companion object {
    private const val VALIDATE_TESTS = "tests"
    private const val VALIDATE_LINKS = "links"

    private const val VALIDATE_TEST_BY_DEFAULT = false
    private const val VALIDATE_LINK_BY_DEFAULT = true
  }
}
