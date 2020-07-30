package com.jetbrains.edu.learning.stepik.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.hasChangedFiles
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.isSignificantlyAfter
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException

class HyperskillCourseUpdater(project: Project, val course: HyperskillCourse) : CourseUpdater(project) {
  private fun HyperskillProject.getCourseFromServer(): HyperskillCourse? {
    val connector = HyperskillConnector.getInstance()
    val hyperskillProject = when (val response = connector.getProject(id)) {
      is Err -> return null
      is Ok -> response.value
    }
    val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language] ?: return null
    val eduEnvironment = eduEnvironment ?: return null
    val stagesFromServer = connector.getStages(id) ?: return null
    return HyperskillCourse(hyperskillProject, languageId, eduEnvironment).apply {
      stages = stagesFromServer
      val lessonFromServer = connector.getLesson(this, hyperskillProject.ideFiles)
      addLesson(lessonFromServer)
      init(this, null, false)
    }
  }

  override fun updateCourse(onFinish: (isUpdated: Boolean) -> Unit) {
    runInBackground(project, EduCoreBundle.message("update.check")) {
      val projectLesson = course.getProjectLesson()
      val courseFromServer = course.hyperskillProject?.getCourseFromServer()
      val projectLessonShouldBeUpdated = courseFromServer != null && projectLesson?.shouldBeUpdated(project, courseFromServer) ?: false
      val codeChallengesUpdates = course.getProblemsLesson()?.getCodeChallengesUpdates() ?: emptyList()

      var isUpdated = false
      if (projectLessonShouldBeUpdated || codeChallengesUpdates.isNotEmpty()) {
        if (HyperskillSettings.INSTANCE.updateAutomatically) {
          doUpdate(courseFromServer, codeChallengesUpdates)
          isUpdated = true
        }
        else {
          showUpdateAvailableNotification(project) {
            runInBackground(project, EduCoreBundle.message("update.process"), false) {
              doUpdate(courseFromServer, codeChallengesUpdates)
            }
          }
        }
      }
      onFinish(isUpdated)
    }
  }

  private fun Lesson.getCodeChallengesUpdates(): List<TaskUpdate> {
    val tasksFromServer = HyperskillConnector.getInstance().getCodeChallenges(this.course, this, taskList.map { it.id })
    val result = mutableListOf<TaskUpdate>()
    for (taskFromServer in tasksFromServer) {
      val localTask = getTask(taskFromServer.id) ?: continue
      if (taskFromServer.updateDate.isSignificantlyAfter(localTask.updateDate)) {
        result.add(TaskUpdate(localTask, taskFromServer))
      }
    }
    return result
  }

  class TaskUpdate(val localTask: Task, val taskFromServer: Task)

  @VisibleForTesting
  fun doUpdate(remoteCourse: HyperskillCourse?, codeChallengesUpdates: List<TaskUpdate>) {
    updateProjectLesson(remoteCourse)
    updateCodeChallenges(codeChallengesUpdates)
    showUpdateCompletedNotification(EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT))
  }

  private fun updateCodeChallenges(codeChallengesUpdates: List<TaskUpdate>) {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) {
        return@invokeAndWaitIfNeeded
      }
      codeChallengesUpdates.forEach {
        val localTask = it.localTask
        if (localTask.status != CheckStatus.Solved) {
          GeneratorUtils.createTaskContent(it.taskFromServer, localTask.getDir(project.courseDir)!!)
        }
        updateTaskDescription(localTask, it.taskFromServer)
        localTask.updateDate = it.taskFromServer.updateDate
        YamlFormatSynchronizer.saveItemWithRemoteInfo(localTask)
      }
    }
  }

  private fun updateProjectLesson(remoteCourse: HyperskillCourse?): Boolean {
    val lesson = course.getProjectLesson() ?: return true
    val remoteLesson = remoteCourse?.getProjectLesson() ?: return true

    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      for ((task, remoteTask) in lesson.taskList.zip(remoteLesson.taskList)) {
        if (!task.updateDate.before(remoteTask.updateDate)) continue

        if (task.status != CheckStatus.Solved) {
          updateFiles(lesson, task, remoteTask)
        }

        updateTaskDescription(task, remoteTask)
        task.updateDate = remoteTask.updateDate
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }

      val courseDir = project.courseDir
      for (additionalFile in remoteCourse.additionalFiles) {
        GeneratorUtils.createChildFile(courseDir, additionalFile.name, additionalFile.text)
      }
    }
    return false
  }

  private fun updateFiles(lesson: FrameworkLesson, task: Task, remoteTask: Task) {
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
        }
        else {
          task.addTaskFile(remoteTaskFile)
          remoteTaskFile
        }

        if (updateInLocalFS) {
          val taskDir = task.getDir(project.courseDir)
          if (taskDir != null) {
            GeneratorUtils.createChildFile(taskDir, path, currentTaskFile.text)
          }
        }
      }
      task.init(course, lesson, false)
    }

    val flm = FrameworkLessonManager.getInstance(project)

    if (lesson.currentTaskIndex != task.index - 1) {
      updateTaskFiles(task, remoteTask.testFiles, false)
      flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.text })
    }
    else {
      // With current logic of next/prev action for hyperskill tasks
      // update of non test files makes sense only for first task
      if (task.index == 1 && !task.hasChangedFiles(project)) {
        updateTaskFiles(task, remoteTask.taskFiles, true)
      }
      else {
        updateTaskFiles(task, remoteTask.testFiles, true)
      }
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(HyperskillCourseUpdater::class.java)

    @JvmStatic
    @VisibleForTesting
    fun Lesson.shouldBeUpdated(project: Project, remoteCourse: HyperskillCourse): Boolean {
      val tasksFromServer = remoteCourse.getProjectLesson()?.taskList ?: return false
      val localTasks = taskList
      return when {
        localTasks.size > tasksFromServer.size -> false
        localTasks.zip(tasksFromServer).any { (task, remoteTask) -> task.id != remoteTask.id } -> false
        localTasks.zip(tasksFromServer).any { (task, remoteTask) -> remoteTask.updateDate.isSignificantlyAfter(task.updateDate) } -> true
        needUpdateCourseAdditionalFiles(project, remoteCourse.additionalFiles) -> true
        else -> false
      }
    }

    private fun needUpdateCourseAdditionalFiles(project: Project, remoteFiles: List<TaskFile>): Boolean {
      val courseDir = project.courseDir
      for (remoteFile in remoteFiles) {
        val needToUpdate = invokeAndWaitIfNeeded {
          runWriteAction {
            if (project.isDisposed) return@runWriteAction false
            val file = courseDir.findFileByRelativePath(remoteFile.name) ?: return@runWriteAction true
            val text = try {
              CCUtils.loadText(file)
            }
            catch (e: IOException) {
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
  }
}

private val Task.testFiles: Map<String, TaskFile>
  get() {
    val testDirs = lesson.course.testDirs
    val defaultTestName = lesson.course.configurator?.testFileName ?: ""
    return taskFiles.filterKeys { path -> path == defaultTestName || testDirs.any { path.startsWith(it) } }
  }

