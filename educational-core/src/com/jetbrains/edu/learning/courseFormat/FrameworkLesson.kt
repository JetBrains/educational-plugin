package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.tasks.Task

class FrameworkLesson() : Lesson() {

  constructor(lesson: Lesson): this() {
    id = lesson.id
    steps = lesson.steps
    is_public = lesson.is_public
    updateDate = lesson.updateDate
    name = lesson.name
    taskList = lesson.taskList
    section = lesson.section
    index = lesson.index
    customPresentableName = lesson.customPresentableName
  }

  var currentTaskIndex: Int = 0

  fun currentTask(): Task = taskList[currentTaskIndex]
}
