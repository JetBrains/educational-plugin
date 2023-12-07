package com.jetbrains.edu.learning.command.validation

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.command.Args
import com.jetbrains.edu.learning.command.CommandResult
import com.jetbrains.edu.learning.command.EduCourseProjectAppStarterBase
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
class EduCourseValidatorAppStarter : EduCourseProjectAppStarterBase() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "validateCourse"

  override val courseMode: CourseMode
    get() = CourseMode.EDUCATOR

  override suspend fun performProjectAction(project: Project, course: Course, args: Args): CommandResult {
    val result = CourseValidationHelper(StdoutServiceMessageConsumer).validate(project, course)

    return if (result) CommandResult.Ok else CommandResult.Error("Some tasks haven't finished successfully")
  }
}
