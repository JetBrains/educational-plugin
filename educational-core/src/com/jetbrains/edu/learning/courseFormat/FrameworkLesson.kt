package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class FrameworkLesson() : Lesson() {

  constructor(lesson: Lesson): this() {
    id = lesson.id
    steps = lesson.steps
    isPublic = lesson.isPublic
    updateDate = lesson.updateDate
    name = lesson.name
    items = lesson.items
    section = lesson.section
    index = lesson.index
    @Suppress("DEPRECATION")
    customPresentableName = lesson.customPresentableName
  }

  var currentTaskIndex: Int = 0
  var isTemplateBased: Boolean = true

  /**
   * currentTask is null when lesson is empty
   */
  fun currentTask(): Task? = taskList.getOrNull(currentTaskIndex)

  override fun getItemType(): String = EduNames.FRAMEWORK
}
