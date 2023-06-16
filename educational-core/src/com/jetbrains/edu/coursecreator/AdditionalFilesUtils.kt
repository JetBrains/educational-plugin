package com.jetbrains.edu.coursecreator

import com.intellij.ide.util.PropertiesComponent
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
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction
import com.jetbrains.edu.learning.*
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
  fun checkIgnoredFiles(project: Project): String? {
    val excludedFiles = loadExcludedFilePaths(project)
    val filesNotFound = excludedFiles.filter { project.courseDir.findFileByRelativePath(it) == null }

    return if (filesNotFound.isNotEmpty()) {
      filesNotFoundErrorMessage("${EduCoreBundle.message("course.creator.error.ignored.files.not.found", EduNames.COURSE_IGNORE)}:",
                                filesNotFound)
    }
    else null
  }

  @Nls
  private fun filesNotFoundErrorMessage(text: @Nls String, filesNotFound: List<String>): String =
    buildString {
      appendLine(text)
      appendLine()
      appendLine(filesNotFound.joinToString())
    }

  fun collectAdditionalFiles(course: Course, project: Project): List<EduFile> {
    ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }

    val fileVisitor = additionalFilesVisitor(project, course)
    VfsUtilCore.visitChildrenRecursively(project.courseDir, fileVisitor)
    return fileVisitor.additionalTaskFiles
  }

  fun inCourseIgnore(file: VirtualFile, project: Project): Boolean {
    return inCourseIgnore(file, project, null)
  }

  private fun inCourseIgnore(file: VirtualFile, project: Project, excludedFiles: List<String>? = null): Boolean {
    val excludedPaths = excludedFiles ?: loadExcludedFilePaths(project)
    val courseRelativePath = VfsUtil.getRelativePath(file, project.courseDir)
    return courseRelativePath in excludedPaths
  }

  private fun isExcluded(file: VirtualFile, excludedFiles: List<String>?, course: Course, project: Project): Boolean =
    inCourseIgnore(file, project, excludedFiles) || excludedByConfigurator(file, course, project)

  private fun excludedByConfigurator(file: VirtualFile, course: Course, project: Project): Boolean =
    course.configurator?.excludeFromArchive(project, file) ?: false

  /**
   * @return list of paths from [EduNames.COURSE_IGNORE] file
   */
  private fun loadExcludedFilePaths(project: Project): List<String> {
    val courseIgnore = project.courseDir.findChild(EduNames.COURSE_IGNORE)
    if (courseIgnore == null || !courseIgnore.exists()) return emptyList()
    return courseIgnore.document.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
  }

  @Suppress("DEPRECATION") // https://youtrack.jetbrains.com/issue/EDU-4930
  fun collectAdditionalLessonInfo(lesson: Lesson, project: Project): LessonAdditionalInfo {
    val nonPluginTasks = lesson.taskList.filter { !it.isPluginTaskType }
    val taskInfo = nonPluginTasks.associateBy(Task::id) {
      TaskAdditionalInfo(it.name, it.customPresentableName, collectTaskFiles(project, it))
    }
    val courseFiles: List<EduFile> = if (lesson.course is HyperskillCourse) collectAdditionalFiles(lesson.course, project) else listOf()
    return LessonAdditionalInfo(lesson.customPresentableName, taskInfo, courseFiles)
  }

  fun getChangeNotesVirtualFile(project: Project): VirtualFile? {
    return project.courseDir.findChild(EduNames.CHANGE_NOTES)
  }

  private fun additionalFilesVisitor(project: Project, course: Course) =
    object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      private val excludedFiles = loadExcludedFilePaths(project)

      val additionalTaskFiles = mutableListOf<EduFile>()
      var archiveLocation = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchiveAction.LAST_ARCHIVE_LOCATION)

      override fun visitFile(file: VirtualFile): Boolean {
        if (FileUtil.toSystemDependentName(file.path) == archiveLocation || isExcluded(file, excludedFiles, course, project)) {
          return false
        }

        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          return file.getTask(project) == null
        }

        addToAdditionalFiles(file, project)
        return false
      }

      private fun addToAdditionalFiles(file: VirtualFile, project: Project) {
        try {
          createAdditionalTaskFile(file, project)?.also { taskFile -> additionalTaskFiles.add(taskFile) }
        }
        catch (e: FileTooBigException) {
          throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong(), false)
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }

      private fun createAdditionalTaskFile(file: VirtualFile, project: Project): EduFile? {
        val taskFile = file.getTaskFile(project)
        if (taskFile != null) return null

        val path = VfsUtilCore.getRelativePath(file, project.courseDir) ?: return null
        return EduFile(path, file.loadEncodedContent())
      }
    }
}
