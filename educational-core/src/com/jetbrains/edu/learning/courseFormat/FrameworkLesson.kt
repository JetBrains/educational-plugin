package com.jetbrains.edu.learning.courseFormat

import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class FrameworkLesson : Lesson() {

  @Transient
  var currentTaskIndex: Int = 0

  override fun getTask(name: String): Task? {
    return if (name == EduNames.TASK) taskList.getOrNull(currentTaskIndex) else null
  }
}
