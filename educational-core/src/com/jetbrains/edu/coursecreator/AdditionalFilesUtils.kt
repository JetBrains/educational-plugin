package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.api.TaskAdditionalInfo
import com.jetbrains.edu.learning.stepik.collectTaskFiles
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.annotations.Nls
import java.io.IOException

object AdditionalFilesUtils {
  private val LOG = Logger.getInstance(AdditionalFilesUtils::class.java)

  /**
   * Existence check for files from [EduNames.COURSE_IGNORE]
   * @return error description
   */
  @JvmStatic
  fun checkIgnoredFiles(project: Project): String? {
    val excludedFiles = loadExcludedFilePaths(project)
    val filesNotFound = excludedFiles.filter { project.courseDir.findFileByRelativePath(it) == null }

    return if (filesNotFound.isNotEmpty()) {
      getFilesNotFoundErrorMessage("${EduCoreBundle.message("course.creator.error.ignored.files.not.found", EduNames.COURSE_IGNORE)}:",
                                   filesNotFound)
    }
    else null
  }

  @Nls
  private fun getFilesNotFoundErrorMessage(text: @Nls String, filesNotFound: List<String>): String =
    buildString {
      appendLine(text)
      appendLine()
      appendLine(filesNotFound.joinToString())
    }

  /**
   * Create list with additional files for course.
   * Method has a lot of checks on belonging.
   */
  @JvmStatic
  fun collectAdditionalFiles(course: Course, project: Project): List<EduFile> {
    ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
    val archiveName = createArchiveName(course.name)
    val additionalFiles = mutableListOf<EduFile>()
    val fileVisitor = virtualFileVisitor(archiveName, project, course, additionalFiles)

    val baseDir = project.courseDir
    VfsUtilCore.visitChildrenRecursively(baseDir, fileVisitor)
    return additionalFiles
  }

  private fun createArchiveName(courseName: String): String {
    val sanitizedName = FileUtil.sanitizeFileName(courseName)
    val courseArchiveName = if (sanitizedName.startsWith("_")) EduNames.COURSE else sanitizedName
    return "${courseArchiveName}.zip"
  }

  private fun virtualFileVisitor(archiveName: String,
                                 project: Project,
                                 course: Course,
                                 additionalFiles: MutableList<EduFile>) =
    object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      private val excludedFiles = loadExcludedFilePaths(project)

      override fun visitFile(file: VirtualFile): Boolean {
        if (file.name == archiveName || // https://youtrack.jetbrains.com/issue/EDU-4912
            isExcluded(file, excludedFiles, course, project)) {
          return false
        }

        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          return file.getTask(project) == null
        }

        addToAdditionalFiles(file, project, additionalFiles)
        return false
      }
    }

  /**
   * Check [file] for ignoring
   */
  @JvmStatic
  fun isExcluded(
    file: VirtualFile,
    excludedFiles: List<String>?,
    course: Course?,
    project: Project
  ): Boolean = isExcludeFromCourseIgnoreFile(file, project, excludedFiles) || isExcludeByConfigurator(file, course, project)

  private fun isExcludeFromCourseIgnoreFile(
    file: VirtualFile,
    project: Project,
    excludedFiles: List<String>? = null
  ): Boolean {
    val excludedPaths = excludedFiles ?: loadExcludedFilePaths(project)
    val courseRelativePath = VfsUtil.getRelativePath(file, project.courseDir)
    return courseRelativePath in excludedPaths
  }

  private fun isExcludeByConfigurator(file: VirtualFile, course: Course?, project: Project): Boolean =
    getConfigurator(course, project)?.excludeFromArchive(project, file) ?: false

  private fun getConfigurator(course: Course?, project: Project): EduConfigurator<*>? = course?.configurator ?: project.course?.configurator

  private fun addToAdditionalFiles(
    file: VirtualFile,
    project: Project,
    additionalFiles: MutableList<EduFile>) {
    try {
      createAdditionalFile(file, project)?.also { taskFile -> additionalFiles.add(taskFile) }
    }
    catch (e: FileTooBigException) {
      throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong(), false)
    }
    catch (e: IOException) {
      LOG.error(e)
    }
  }

  private fun createAdditionalFile(
    file: VirtualFile,
    project: Project
  ): EduFile? {
    val taskFile = file.getTaskFile(project)
    if (taskFile != null) return null

    val baseDir = project.courseDir
    val path = VfsUtilCore.getRelativePath(file, baseDir) ?: return null
    return EduFile(path, file.loadEncodedContent())
  }

  /**
   * @return list of paths from [EduNames.COURSE_IGNORE] file
   */
  private fun loadExcludedFilePaths(project: Project): List<String> {
    val courseIgnore = project.courseDir.findChild(EduNames.COURSE_IGNORE)
    if (courseIgnore == null || !courseIgnore.exists()) return emptyList()
    return courseIgnore.document.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
  }

  @JvmStatic
  @Suppress("deprecation", "https://youtrack.jetbrains.com/issue/EDU-4930")
  fun collectAdditionalLessonInfo(lesson: Lesson, project: Project): LessonAdditionalInfo {
    val nonPluginTasks = lesson.taskList.filter { !it.isPluginTaskType }
    val taskInfo = nonPluginTasks.associateBy(Task::id) {
      TaskAdditionalInfo(it.name, it.customPresentableName, collectTaskFiles(project, it))
    }
    val courseFiles: List<EduFile> = if (lesson.course is HyperskillCourse) collectAdditionalFiles(lesson.course, project) else listOf()
    return LessonAdditionalInfo(lesson.customPresentableName, taskInfo, courseFiles)
  }
}