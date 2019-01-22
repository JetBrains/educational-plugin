package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class FrameworkLesson() : Lesson() {

  constructor(lesson: Lesson): this() {
    id = lesson.id
    steps = lesson.steps
    tags = lesson.tags
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

  fun prepareNextTask(project: Project, taskDir: VirtualFile) {
    FrameworkLessonManager.getInstance(project).prepareNextTask(this, taskDir)
  }

  fun preparePrevTask(project: Project, taskDir: VirtualFile) {
    FrameworkLessonManager.getInstance(project).preparePrevTask(this, taskDir)
  }
}
