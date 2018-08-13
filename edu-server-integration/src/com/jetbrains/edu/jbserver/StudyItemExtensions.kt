package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun StudyItem.addMetaInformation(meta: StudyItem): Unit = when (this) {
  is EduCourse -> {
    if (meta !is EduCourse) throw IllegalArgumentException()
    this.courseId = meta.courseId
    for ((el, meta) in (this.items zip meta.items)) {
      el.addMetaInformation(meta)
    }
  }
  is Section -> {
    if (meta !is Section) throw IllegalArgumentException()
    this.id = meta.id
    for ((el, meta) in (this.items zip meta.items)) {
      el.addMetaInformation(meta)
    }
  }
  is Lesson -> {
    if (meta !is Lesson) throw IllegalArgumentException()
    this.id = meta.id
    for ((el, meta) in (this.taskList zip meta.taskList)) {
      el.addMetaInformation(meta)
    }
  }
  is Task -> {
    if (meta !is Task) throw IllegalArgumentException()
    this.stepId = meta.stepId
  }
  else -> {
  }
}
