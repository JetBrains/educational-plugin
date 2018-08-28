@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourseRemoteInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import java.util.*

val StepikCourse.isAdaptive: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isAdaptive ?: false
val StepikCourse.isCompatible: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isIdeaCompatible ?: false
val StepikCourse.id: Int get() = (remoteInfo as? StepikCourseRemoteInfo)?.id ?: 0

var StepikCourse.updateDate: Date get() = (remoteInfo as? StepikCourseRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    (remoteInfo as? StepikCourseRemoteInfo)?.updateDate = date
}

val StudyItem.id: Int get() = (this as? StepikCourse)?.id ?: (this as? Section)?.id ?: (this as? Lesson)?.id ?: (this as? Task)?.id ?: 0

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
