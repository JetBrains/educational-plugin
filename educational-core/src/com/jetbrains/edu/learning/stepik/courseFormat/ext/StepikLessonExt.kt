@file:JvmName("StepikLessonExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikLessonRemoteInfo
import java.util.*

var Lesson.unitId: Int
  get() = (remoteInfo as? StepikLessonRemoteInfo)?.unitId ?: 0
  set(unitId) {
    stepikRemoteInfo.unitId = unitId
  }

var Lesson.id: Int
  get() = (remoteInfo as? StepikLessonRemoteInfo)?.id ?: 0
  set(id) {
    stepikRemoteInfo.id = id
  }

var Lesson.updateDate: Date
  get() = (remoteInfo as? StepikLessonRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    stepikRemoteInfo.updateDate = date
  }

private val Lesson.stepikRemoteInfo : StepikLessonRemoteInfo
  get() {
    if (remoteInfo !is StepikLessonRemoteInfo) {
      remoteInfo = StepikLessonRemoteInfo()
    }
    return remoteInfo as StepikLessonRemoteInfo
  }

val Lesson.steps: List<Int>
  get() = (remoteInfo as? StepikLessonRemoteInfo)?.steps ?: listOf()

fun Lesson.getTask(id: Int): Task? {
  for (task in taskList) {
    if (task.stepId == id) {
      return task
    }
  }
  return null
}

fun isStepikLesson(lesson: Lesson): Boolean {
  return lesson.remoteInfo is StepikLessonRemoteInfo
}
