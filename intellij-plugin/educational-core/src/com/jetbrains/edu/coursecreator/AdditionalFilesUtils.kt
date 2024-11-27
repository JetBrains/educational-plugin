package com.jetbrains.edu.coursecreator

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.jetbrains.edu.coursecreator.actions.BinaryContentsFromDisk
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction
import com.jetbrains.edu.coursecreator.actions.TextualContentsFromDisk
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.api.TaskAdditionalInfo
import com.jetbrains.edu.learning.stepik.collectTaskFiles
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import java.io.IOException

object AdditionalFilesUtils {
  private val LOG = Logger.getInstance(AdditionalFilesUtils::class.java)

  /**
   * @param saveDocuments specifies whether to save all documents in open editors before collecting additional files.
   * Default is `true`.
   * If [saveDocuments] is `false`, no write action will be performed during the call.
   *
   * @param detectTaskFoldersByContents controls the way to determine whether a folder is a task folder.
   * Additional files are not searched inside task folders. Default is `false`.
   * When [detectTaskFoldersByContents] is `true`, the folder is considered to be a task folder, if it contains
   * the `task-info.yaml` file.
   * When [detectTaskFoldersByContents] is `false`, the `project.course` object is inspected to find the task with that folder.
   * Use `detectTaskFoldersByContents=true` in case the `project.course` object is unavailable.
   */
  fun collectAdditionalFiles(
    courseConfigurator: EduConfigurator<*>?,
    project: Project,
    saveDocuments: Boolean = true,
    detectTaskFoldersByContents: Boolean = false
  ): List<EduFile> {
    if (courseConfigurator == null) return listOf()

    if (saveDocuments) {
      ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
    }

    val fileVisitor = additionalFilesVisitor(project, courseConfigurator, detectTaskFoldersByContents)
    VfsUtilCore.visitChildrenRecursively(project.courseDir, fileVisitor)
    return fileVisitor.additionalTaskFiles
  }

  /**
   * Tests the file to be excluded either by a course configurator or a .courseignore
   */
  fun isExcluded(
    file: VirtualFile,
    courseIgnoreRules: CourseIgnoreRules,
    courseConfigurator: EduConfigurator<*>,
    project: Project
  ): Boolean {
    return courseIgnoreRules.isIgnored(file) ||
           courseConfigurator.excludeFromArchive(project, file) ||
           FileUtil.toSystemDependentName(file.path) == PropertiesComponent.getInstance(project)
             .getValue(CCCreateCourseArchiveAction.LAST_ARCHIVE_LOCATION)
  }

  @Suppress("DEPRECATION") // https://youtrack.jetbrains.com/issue/EDU-4930
  fun collectAdditionalLessonInfo(lesson: Lesson, project: Project): LessonAdditionalInfo {
    val nonPluginTasks = lesson.taskList.filter { !it.isPluginTaskType }
    val taskInfo = nonPluginTasks.associateBy(Task::id) {
      TaskAdditionalInfo(it.name, it.customPresentableName, collectTaskFiles(project, it))
    }
    val courseFiles: List<EduFile> = if (lesson.course is HyperskillCourse) {
      collectAdditionalFiles(lesson.course.configurator, project)
    }
    else {
      listOf()
    }
    return LessonAdditionalInfo(lesson.customPresentableName, taskInfo, courseFiles)
  }

  fun getChangeNotesVirtualFile(project: Project): VirtualFile? {
    return project.courseDir.findChild(EduNames.CHANGE_NOTES)
  }

  private fun additionalFilesVisitor(project: Project, courseConfigurator: EduConfigurator<*>, detectTaskFoldersByContents: Boolean) =
    object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      // we take the course ignore rules once, and we are sure they are not changed while course archive is being created
      private val courseIgnoreRules = CourseIgnoreRules.loadFromCourseIgnoreFile(project)

      val additionalTaskFiles = mutableListOf<EduFile>()

      override fun visitFile(file: VirtualFile): Boolean {
        if (isExcluded(
            file,
            courseIgnoreRules,
            courseConfigurator,
            project
          )
        ) {
          return false
        }

        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          val isTaskFolder = if (detectTaskFoldersByContents) {
            file.findChild(TASK_CONFIG) != null
          }
          else {
            file.getTask(project) != null
          }

          return !isTaskFolder
        }

        addToAdditionalFiles(file, project)
        return false
      }

      private fun addToAdditionalFiles(file: VirtualFile, project: Project) {
        try {
          createAdditionalTaskFile(file, project)?.also { taskFile -> additionalTaskFiles.add(taskFile) }
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }

      private fun createAdditionalTaskFile(file: VirtualFile, project: Project): EduFile? {
        val path = VfsUtilCore.getRelativePath(file, project.courseDir) ?: return null
        val contents = if (file.isToEncodeContent) {
          BinaryContentsFromDisk(file)
        }
        else {
          TextualContentsFromDisk(file)
        }
        return EduFile(path, contents)
      }
    }
}
