package com.jetbrains.edu.learning.framework

import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

/**
 * Corresponds to the way with which a learner navigates between tasks of the framework lesson.
 * In other words, this value influences what contents of files learners see when they open the next task of a framework lesson.
 * If the returned value is `false`, a learner sees the files prepared by the course author.
 * This behavior is the same as for non-framework lessons.
 * If the returned value is `true`, the files changed by a learner stay the same.
 * Only files that are non-propagatable (either invisible or readonly) have the contents specified by the course author.
 *
 * This property is backed by the `isTemplateBased` property, but the latter could be specified
 * for either lesson or a course.
 */
val FrameworkLesson.propagateFilesOnNavigation: Boolean
  get() {
    val thisCourse = course
    return !isTemplateBased || thisCourse is HyperskillCourse && !thisCourse.isTemplateBased
  }