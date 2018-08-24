package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun StudyItem.addMetaInformation(meta: StudyItem): Unit = when (this) {
  is EduCourse -> {
    if (meta !is EduCourse) throw ServerException("wrong response format")
    courseId = meta.courseId
    for ((el, metaEl) in (items zip meta.items)) {
      el.addMetaInformation(metaEl)
    }
    stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  }
  is Section -> {
    if (meta !is Section) throw ServerException("wrong response format")
    id = meta.id
    for ((el, metaEl) in (items zip meta.items)) {
      el.addMetaInformation(metaEl)
    }
    stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  }
  is Lesson -> {
    if (meta !is Lesson) throw ServerException("wrong response format")
    id = meta.id
    for ((el, metaEl) in (taskList zip meta.taskList)) {
      el.addMetaInformation(metaEl)
    }
    stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  }
  is Task -> {
    if (meta !is Task) throw ServerException("wrong response format")
    stepId = meta.stepId
    stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  }
  else -> {}
}

fun StudyItem.globalSetChangeStatus(status: StepikChangeStatus) {
  stepikChangeStatus = status
  when (this) {
    is EduCourse -> items.forEach { it.globalSetChangeStatus(status) }
    is Section -> items.forEach { it.globalSetChangeStatus(status) }
    is Lesson -> taskList.forEach { it.globalSetChangeStatus(status) }
  }
}
