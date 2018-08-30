@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.intellij.openapi.util.Ref
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo
import java.util.*

val StepikCourse.isAdaptive: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isAdaptive ?: false
val StepikCourse.isCompatible: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isIdeaCompatible ?: false
val StepikCourse.id: Int get() = (remoteInfo as? StepikCourseRemoteInfo)?.id ?: 0

var StepikCourse.updateDate: Date get() = (remoteInfo as? StepikCourseRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    (remoteInfo as? StepikCourseRemoteInfo)?.updateDate = date
  }


fun StepikCourse.getTask(stepId: Int): Task? {
  val taskRef = Ref<Task>()
  course.visitLessons { lesson ->
    val task = lesson.getTask(stepId)
    if (task != null) {
      taskRef.set(task)
      return@visitLessons false
    }
    true
  }
  return null
}