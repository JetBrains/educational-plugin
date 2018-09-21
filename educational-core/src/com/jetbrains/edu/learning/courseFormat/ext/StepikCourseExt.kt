@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*

val RemoteCourse.isAdaptive: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isAdaptive ?: false
val RemoteCourse.isCompatible: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isIdeaCompatible ?: false
val RemoteCourse.id: Int get() = (remoteInfo as? StepikRemoteInfo)?.id ?: 0

var RemoteCourse.updateDate: Date get() = (remoteInfo as? StepikRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
  (remoteInfo as? StepikRemoteInfo)?.updateDate = date
}

val StudyItem.id: Int get() = (this as? RemoteCourse)?.id ?: (this as? Section)?.id ?: (this as? Lesson)?.id ?: (this as? Task)?.id ?: 0

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.asSequence().filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
