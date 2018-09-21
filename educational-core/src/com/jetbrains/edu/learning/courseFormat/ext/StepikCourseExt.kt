@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import java.util.*

val StepikCourse.isAdaptive: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isAdaptive ?: false
val StepikCourse.isCompatible: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isIdeaCompatible ?: false
val StepikCourse.id: Int get() = (remoteInfo as? StepikRemoteInfo)?.id ?: 0

var StepikCourse.updateDate: Date get() = (remoteInfo as? StepikRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
  (remoteInfo as? StepikRemoteInfo)?.updateDate = date
}

val StudyItem.id: Int get() = (this as? StepikCourse)?.id ?: (this as? Section)?.id ?: (this as? Lesson)?.id ?: (this as? Task)?.id ?: 0

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.asSequence().filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
