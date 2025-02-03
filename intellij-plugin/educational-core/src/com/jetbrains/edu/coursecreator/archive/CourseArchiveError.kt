package com.jetbrains.edu.coursecreator.archive

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.jetbrains.annotations.Nls
import java.io.FileNotFoundException

interface CourseArchiveError {

  val message: @Nls String

  /**
   * Action which is supposed to be performed without additional user actions
   */
  @RequiresEdt
  fun immediateAction(project: Project) {}
}

abstract class ExceptionCourseArchiveError<T : Throwable>(val exception: T) : CourseArchiveError {
  override val message: String
    get() = exception.message.orEmpty()
}

class HugeBinaryFileError(e: HugeBinaryFileException) : ExceptionCourseArchiveError<HugeBinaryFileException>(e)
class BrokenPlaceholderError(e: BrokenPlaceholderException) : ExceptionCourseArchiveError<BrokenPlaceholderException>(e) {
  override fun immediateAction(project: Project) {
    val yamlFile = exception.placeholder.taskFile.task.getDir(project.courseDir)?.findChild(TASK_CONFIG) ?: return
    FileEditorManager.getInstance(project).openFile(yamlFile, true)
  }
}
// TODO: use more specific exception for error related to additional files.
//  `FileNotFoundException` is not related to additional files
//  and in theory may occur in other cases as well
class AdditionalFileNotFoundError(e: FileNotFoundException) : ExceptionCourseArchiveError<FileNotFoundException>(e)
class OtherError(e: Throwable, private val errorMessage: @Nls String? = null) : ExceptionCourseArchiveError<Throwable>(e) {
  override val message: String
    get() = errorMessage ?: EduCoreBundle.message("error.failed.to.create.course.archive.notification.title")
}
