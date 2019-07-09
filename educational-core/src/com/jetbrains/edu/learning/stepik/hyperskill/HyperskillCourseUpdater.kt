package com.jetbrains.edu.learning.stepik.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.hasChangedFiles
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import java.io.IOException

object HyperskillCourseUpdater {

  private val LOG: Logger = Logger.getInstance(HyperskillCourseUpdater::class.java)

  @JvmStatic
  fun updateCourse(project: Project, course: HyperskillCourse) {
    ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(project, "Checking for Course Updates") {

      override fun run(indicator: ProgressIndicator) {
        val projectId = course.hyperskillProject.id
        val connector = HyperskillConnector.getInstance()
        val hyperskillProject = connector.getProject(projectId) ?: return
        val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language] ?: return
        val remoteCourse = HyperskillCourse(hyperskillProject, languageId)
        remoteCourse.stages = connector.getStages(projectId) ?: return
        val remoteLesson = connector.getLesson(remoteCourse, hyperskillProject.ideFiles) ?: return
        remoteCourse.addLesson(remoteLesson)
        remoteCourse.init(null, null, false)

        if (course.canBeUpdated(project, remoteCourse)) {
          showUpdateAvailableNotification(project) {
            updateCourse(project, course, remoteCourse)
          }
        }
      }
    })
  }

  private fun HyperskillCourse.canBeUpdated(project: Project, remoteCourse: HyperskillCourse): Boolean {
    val lesson = lessons.firstOrNull() as? FrameworkLesson ?: return false
    val remoteLesson = remoteCourse.lessons.firstOrNull() as? FrameworkLesson ?: return false
    val tasks = lesson.taskList
    val remoteTasks = remoteLesson.taskList

    return when {
      tasks.size > remoteTasks.size -> false
      tasks.zip(remoteTasks).any { (task, remoteTask) -> task.id != remoteTask.id } -> false
      tasks.zip(remoteTasks).any { (task, remoteTask) -> task.updateDate.before(remoteTask.updateDate) } -> true
      needUpdateCourseAdditionalFiles(project, remoteCourse.additionalFiles) -> true
      else -> false
    }
  }

  private fun needUpdateCourseAdditionalFiles(project: Project, remoteFiles: List<TaskFile>): Boolean {
    val courseDir = project.courseDir
    for (remoteFile in remoteFiles) {
      // BACKCOMPAT: 2018.3
      @Suppress("DEPRECATION")
      val needToUpdate = invokeAndWaitIfNeed {
        runWriteAction {
          if (project.isDisposed) return@runWriteAction false
          val file = courseDir.findFileByRelativePath(remoteFile.name) ?: return@runWriteAction true
          val text = try {
            CCUtils.loadText(file)
          } catch (e: IOException) {
            LOG.warn("Failed to load text of `${remoteFile.name}` additional file", e)
            return@runWriteAction true
          }
          text != remoteFile.text
        }
      }
      if (needToUpdate) return true
    }

    return false
  }

  @JvmStatic
  @VisibleForTesting
  fun updateCourse(project: Project, currentCourse: HyperskillCourse, remoteCourse: HyperskillCourse) {
    val lesson = currentCourse.lessons.firstOrNull() as? FrameworkLesson ?: return
    val remoteLesson = remoteCourse.lessons.firstOrNull() as? FrameworkLesson ?: return

    fun updateTaskFiles(
      task: Task,
      remoteTaskFiles: Map<String, TaskFile>,
      updateInLocalFS: Boolean
    ) {
      val taskFiles = task.taskFiles
      for ((path, remoteTaskFile) in remoteTaskFiles) {
        val taskFile = taskFiles[path]
        val currentTaskFile = if (taskFile != null) {
          taskFile.setText(remoteTaskFile.text)
          taskFile
        } else {
          task.addTaskFile(remoteTaskFile)
          remoteTaskFile
        }

        if (updateInLocalFS) {
          val taskDir = task.getTaskDir(project)
          if (taskDir != null) {
            GeneratorUtils.createChildFile(taskDir, path, currentTaskFile.text)
          }
        }
      }
      task.init(currentCourse, lesson, false)
    }

    // BACKCOMPAT: 2018.3
    @Suppress("DEPRECATION")
    invokeAndWaitIfNeed {
      if (project.isDisposed) return@invokeAndWaitIfNeed
      val flm = FrameworkLessonManager.getInstance(project)

      for ((task, remoteTask) in lesson.taskList.zip(remoteLesson.taskList)) {
        if (!task.updateDate.before(remoteTask.updateDate)) continue

        if (task.status == CheckStatus.Solved) continue

        if (lesson.currentTaskIndex != task.index - 1) {
          updateTaskFiles(task, remoteTask.testFiles, false)
          flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.text } )
        } else {
          // With current logic of next/prev action for hyperskill tasks
          // update of non test files makes sense only for first task
          if (task.index == 1 && !task.hasChangedFiles(project)) {
            updateTaskFiles(task, remoteTask.taskFiles, true)
          } else {
            updateTaskFiles(task, remoteTask.testFiles, true)
          }
        }

        task.descriptionText = remoteTask.descriptionText
        task.descriptionFormat = remoteTask.descriptionFormat
        task.updateDate = remoteTask.updateDate
      }

      val courseDir = project.courseDir
      for (additionalFile in remoteCourse.additionalFiles) {
        GeneratorUtils.createChildFile(courseDir, additionalFile.name, additionalFile.text)
      }
    }
  }

}

private val Task.testFiles: Map<String, TaskFile> get() {
  val testDirs = lesson.course.testDirs
  val defaultTestName = lesson.course.configurator?.testFileName ?: ""
  return taskFiles.filterKeys { path -> path == defaultTestName || testDirs.any { path.startsWith(it) } }
}
