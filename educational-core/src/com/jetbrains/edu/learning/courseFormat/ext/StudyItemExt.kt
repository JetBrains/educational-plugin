@file:JvmName("StudyItemExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val StudyItem.typeId: String
  get() {
    return when (this) {
      is Course -> courseType
      is Section -> "section"
      is FrameworkLesson -> "framework"
      is Lesson -> "lesson"
      is Task -> taskType
      else -> error("Unknown StudyItem: ${this.javaClass.name}")
    }
  }