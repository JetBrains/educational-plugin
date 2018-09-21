@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Course.isAdaptive: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isAdaptive ?: false
val Course.isCompatible: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isIdeaCompatible ?: false
val Course.id: Int get() = (remoteInfo as? StepikRemoteInfo)?.id ?: 0

val StudyItem.id: Int get() = (this as? Course)?.id ?: (this as? Section)?.id ?: (this as? Lesson)?.id ?: (this as? Task)?.id ?: 0

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.asSequence().filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
