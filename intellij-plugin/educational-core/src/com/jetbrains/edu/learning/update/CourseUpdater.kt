package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.update.elements.CourseUpdate
import com.jetbrains.edu.learning.update.elements.StudyItemUpdate

abstract class CourseUpdater(val project: Project, private val localCourse: Course) : ItemUpdater<Course> {
  protected abstract fun createLessonUpdater(container: LessonContainer): LessonUpdater
  protected abstract fun createSectionUpdater(course: Course): SectionUpdater

  suspend fun collect(remoteCourse: Course): List<StudyItemUpdate<StudyItem>> {
    val updates = mutableListOf<StudyItemUpdate<StudyItem>>()

    val sectionUpdater = createSectionUpdater(localCourse)
    val sectionUpdates = sectionUpdater.collect(remoteCourse)
    updates.addAll(sectionUpdates)

    val lessonUpdater = createLessonUpdater(localCourse)
    val lessonUpdates = lessonUpdater.collect(remoteCourse)
    updates.addAll(lessonUpdates)

    if (localCourse.isOutdated(remoteCourse) || isCourseChanged(localCourse, remoteCourse)) {
      updates.add(CourseUpdate.get(localCourse, remoteCourse))
    }

    return updates
  }

  suspend fun update(remoteCourse: Course) {
    val updates = collect(remoteCourse)
    updates.forEach {
      it.update(project)
    }
  }

  abstract fun isCourseChanged(localCourse: Course, remoteCourse: Course): Boolean

  protected fun Course.isChanged(remoteCourse: Course): Boolean =
    when {
      name != remoteCourse.name -> true
      additionalFiles != remoteCourse.additionalFiles -> true
      else -> false
    }
}