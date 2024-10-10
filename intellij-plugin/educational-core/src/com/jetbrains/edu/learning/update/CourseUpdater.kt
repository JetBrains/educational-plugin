package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.update.comparators.EduFileComparator.Companion.areNotEqual
import com.jetbrains.edu.learning.update.elements.CourseUpdate
import com.jetbrains.edu.learning.update.elements.StudyItemUpdate

abstract class CourseUpdater<T : Course>(val project: Project, private val localCourse: T) : ItemUpdater<T> {
  protected abstract fun createLessonUpdater(container: LessonContainer): LessonUpdater
  protected abstract fun createSectionUpdater(course: T): SectionUpdater

  suspend fun collect(remoteCourse: T): Collection<StudyItemUpdate<StudyItem>> {
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

  suspend fun update(remoteCourse: T) {
    val updates = collect(remoteCourse)
    updates.forEach {
      it.update(project)
    }
  }

  abstract fun isCourseChanged(localCourse: T, remoteCourse: T): Boolean

  protected fun T.isChanged(remoteCourse: T): Boolean =
    when {
      name != remoteCourse.name -> true
      additionalFiles areNotEqual remoteCourse.additionalFiles -> true
      else -> false
    }
}