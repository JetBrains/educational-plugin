package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CourseArchiveIndicator {

  private var indicator: ProgressIndicator? = null

  private var filesCount: Int = 0
  private var writtenFiles = 0
  private var folderToLocalizePaths: VirtualFile? = null

  fun init(folderToLocalizePaths: VirtualFile, course: Course, indicator: ProgressIndicator?) {
    this.folderToLocalizePaths = folderToLocalizePaths
    this.indicator = indicator
    indicator?.isIndeterminate = false

    var taskFilesCount = 0
    course.visitTasks { taskFilesCount += it.taskFiles.size }

    val additionalFilesCount = course.additionalFiles.size

    filesCount = taskFilesCount + additionalFilesCount
  }

  fun writeFile(file: EduFile) {
    val filePath = file.pathInCourse

    indicator?.checkCanceled()

    writtenFiles++

    indicator?.fraction = if (filesCount != 0) writtenFiles.toDouble() / filesCount else 0.0
    indicator?.text = EduCoreBundle.message("action.create.course.archive.writing.file.no", writtenFiles, filesCount)
    indicator?.text2 = EduCoreBundle.message("action.create.course.archive.writing.file.name", filePath)
  }
}