package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun StudyItem.addMetaInformation(meta: StudyItem): Unit = when (this) {
  is EduCourse -> {
    if (meta !is EduCourse) throw ServerException("wrong response format")
    courseId = meta.courseId
    for ((el, metaEl) in (items zip meta.items)) {
      el.addMetaInformation(metaEl)
    }
    isUploaded = true
  }
  is Section -> {
    if (meta !is Section) throw ServerException("wrong response format")
    id = meta.id
    for ((el, metaEl) in (items zip meta.items)) {
      el.addMetaInformation(metaEl)
    }
    isUploaded = true
  }
  is Lesson -> {
    if (meta !is Lesson) throw ServerException("wrong response format")
    id = meta.id
    for ((el, metaEl) in (taskList zip meta.taskList)) {
      el.addMetaInformation(metaEl)
    }
    isUploaded = true
  }
  is Task -> {
    if (meta !is Task) throw ServerException("wrong response format")
    stepId = meta.stepId
    isUploaded = true
  }
  else -> {}
}
