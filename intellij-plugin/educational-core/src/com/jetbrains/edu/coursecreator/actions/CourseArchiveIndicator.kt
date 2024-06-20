package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.messages.EduCoreBundle

enum class FileCountingMode {
  DURING_READ,
  DURING_WRITE
}

class CourseArchiveIndicator(private val countingMode: FileCountingMode) {

  private var indicator: ProgressIndicator? = null

  private var filesCount: Int = 0
  private var readFiles = 0
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

  fun readFile(file: VirtualFile) {
    if (countingMode == FileCountingMode.DURING_READ) {
      val filePath = folderToLocalizePaths?.let { VfsUtil.getRelativePath(file, it) } ?: file.path
      processFile(filePath)
    }
  }

  fun writeFile(file: EduFile) {
    if (countingMode == FileCountingMode.DURING_WRITE) {
      processFile(file.pathInCourse)
    }
  }

  private fun processFile(filePath: String) {
    indicator?.checkCanceled()

    readFiles++

    indicator?.fraction = if (filesCount != 0) readFiles.toDouble() / filesCount else 0.0
    indicator?.text = EduCoreBundle.message("action.create.course.archive.writing.file.no", readFiles, filesCount)
    indicator?.text2 = EduCoreBundle.message("action.create.course.archive.writing.file.name", filePath)
  }
}