package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Contains general information about course: [Course] object itself and where it's located.
 * It's supposed to be used for course-related generation like generation of whole course content or new task creation
 *
 * Main purpose of this class to have common interface for two contexts:
 * - [Project] object is not available but [course] and its [courseDir] are already known.
 *   For example, course content generation
 * - [Project] is available and [Course] and its root directory can be retrieved from project itself
 *
 * Generic parameter [T] is introduced to require proper nullability of [course] object
 * and avoid unexpected checks of [course] for null
 */
sealed interface CourseInfoHolder<T : Course?> {

  val course: T
  val courseDir: VirtualFile

  companion object {
    fun fromProject(project: Project): CourseInfoHolder<Course?> = ProjectCourseInfoHolder(project)
    fun <T : Course> fromCourse(course: T, courseDir: VirtualFile): CourseInfoHolder<T> = CourseInfoHolderImpl(course, courseDir)
  }
}

private class ProjectCourseInfoHolder(private val project: Project) : CourseInfoHolder<Course?> {
  override val course: Course?
    get() = project.course
  override val courseDir: VirtualFile
    get() = project.courseDir
}


private class CourseInfoHolderImpl<T : Course>(override val course: T, override val courseDir: VirtualFile) : CourseInfoHolder<T>

fun Project.toCourseInfoHolder(): CourseInfoHolder<Course?> = CourseInfoHolder.fromProject(this)
