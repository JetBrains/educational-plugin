package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CourseArchiveIndicator {

  private var indicator: ProgressIndicator? = null

  private var filesCount: Int = 0
  private var readFiles = 0

  fun init(course: Course, indicator: ProgressIndicator) {
    this.indicator = indicator
    indicator.isIndeterminate = false

    var taskFilesCount = 0
    course.visitTasks { taskFilesCount += it.taskFiles.size }

    val additionalFilesCount = course.additionalFiles.size

    filesCount = taskFilesCount + additionalFilesCount
  }

  fun readFile(file: VirtualFile) {
    indicator?.checkCanceled()

    readFiles++

    indicator?.fraction = if (filesCount != 0) readFiles.toDouble() / filesCount else 0.0
    indicator?.text = EduCoreBundle.message("action.create.course.archive.writing.file.no", readFiles, filesCount)
    indicator?.text2 = EduCoreBundle.message("action.create.course.archive.writing.file.name", file)
  }
}