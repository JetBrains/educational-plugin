package com.jetbrains.edu.commandLine

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

/**
 * Adds `createCourse` command for IDE to create a course project.
 *
 * Expected usages:
 * - `createCourse %/path/to/project/dir% --marketplace %marketplace-course-link%`
 * - `createCourse %/path/to/project/dir% --archive %/path/to/course/archive%`
 */
class EduCourseCreatorAppStarter : EduCourseProjectAppStarterBase() {

  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "createCourse"

  override val courseMode: CourseMode
    get() = CourseMode.STUDENT

  override suspend fun performProjectAction(project: Project, course: Course, args: Args): CommandResult = CommandResult.Ok
}
