package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.hasChangedFiles
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.eduEnvironment
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import java.util.*

class HyperskillCourseUpdater(project: Project, val course: HyperskillCourse) : CourseUpdater(project) {
  private fun HyperskillProject.getCourseFromServer(): HyperskillCourse? {
    val connector = HyperskillConnector.getInstance()
    val hyperskillProject = when (val response = connector.getProject(id)) {
      is Err -> return null
      is Ok -> response.value
    }
    val languageId = HyperskillLanguages.getEduLanguage(hyperskillProject.language) ?: return null
    val eduEnvironment = eduEnvironment ?: return null
    val stagesFromServer = connector.getStages(id) ?: return null
    return HyperskillCourse(hyperskillProject, languageId, eduEnvironment).apply {
      stages = stagesFromServer
      val lessonFromServer = connector.getLesson(this, hyperskillProject.ideFiles)
      addLesson(lessonFromServer)
      init(this, false)
    }
  }

  override fun updateCourse(onFinish: (isUpdated: Boolean) -> Unit) {
    fun getProblemsUpdate(): List<TaskUpdate> {
      val legacyProblemLesson = course.getProblemsLesson()
      val newProblemLessons = course.getTopicsSection()?.lessons ?: emptyList()
      val problemLessons = listOfNotNull(legacyProblemLesson, *newProblemLessons.toTypedArray())
      return problemLessons.flatMap { lesson -> lesson.getProblemsUpdates() }
    }

    runInBackground(project, EduCoreBundle.message("update.check")) {
      val projectLesson = course.getProjectLesson()
      val courseFromServer = course.hyperskillProject?.getCourseFromServer()
      val hyperskillProject = course.hyperskillProject
      val projectShouldBeUpdated = hyperskillProject != null && hyperskillProject.shouldBeUpdated(courseFromServer?.hyperskillProject)
      val projectLessonShouldBeUpdated = courseFromServer != null && projectLesson?.shouldBeUpdated(project, courseFromServer) ?: false
      val problemsUpdates = getProblemsUpdate()

      var isUpdated = false
      if (projectShouldBeUpdated || projectLessonShouldBeUpdated || problemsUpdates.isNotEmpty()) {
        if (HyperskillSettings.INSTANCE.updateAutomatically) {
          doUpdate(courseFromServer, problemsUpdates)
          isUpdated = true
        }
        else {
          showUpdateAvailableNotification(project) {
            runInBackground(project, EduCoreBundle.message("update.process"), false) {
              doUpdate(courseFromServer, problemsUpdates)
            }
          }
        }
      }
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded

        course.updateDate = Date()
        YamlFormatSynchronizer.saveRemoteInfo(course)
        onFinish(isUpdated)
      }
    }
  }

  private fun HyperskillProject.shouldBeUpdated(hyperskillProject: HyperskillProject?): Boolean {
    return when {
      hyperskillProject != null && title != hyperskillProject.title -> true
      else -> false
    }
  }

  private fun Lesson.getProblemsUpdates(): List<TaskUpdate> {
    val tasksFromServer = HyperskillConnector.getInstance().getProblems(this.course, this, taskList.map { it.id })
    val localTasks = taskList.associateBy { it.id }

    val result = mutableListOf<TaskUpdate>()
    for (serverTask in tasksFromServer) {
      val localTask = localTasks[serverTask.id]
      if (localTask != null) {
        val localTaskIsExpired = serverTask.updateDate.isSignificantlyAfter(localTask.updateDate)
        val serverTaskIsDifferent = taskIsDifferent(localTask, serverTask)
        if (localTaskIsExpired || serverTaskIsDifferent) {
          result.add(TaskUpdate(localTask, serverTask))
        }
      }
    }
    return result
  }

  /**
   * Tasks can be different when tasks have different description.
   * It can happen because of bugs
   */
  private fun taskIsDifferent(first: Task, second: Task): Boolean {
    var result = first.descriptionText != second.descriptionText ||
                 first.feedbackLink != second.feedbackLink ||
                 first.name != second.name

    if (first is ChoiceTask && second is ChoiceTask) {
      result = result || first.choiceOptions != second.choiceOptions
    }
    if (first is RemoteEduTask && second is RemoteEduTask) {
      result = result || first.checkProfile != second.checkProfile
    }
    return result
  }

  class TaskUpdate(val localTask: Task, val taskFromServer: Task)

  @VisibleForTesting
  fun doUpdate(remoteCourse: HyperskillCourse?, problemsUpdates: List<TaskUpdate>) {
    if (remoteCourse != null) {
      updateCourse(remoteCourse)
      updateProjectLesson(remoteCourse)
    }
    updateProblems(problemsUpdates)
    showUpdateCompletedNotification(EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT))
  }

  private fun updateCourse(remoteCourse: HyperskillCourse) {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      course.description = remoteCourse.description
      course.hyperskillProject ?: return@invokeAndWaitIfNeeded
      remoteCourse.hyperskillProject ?: return@invokeAndWaitIfNeeded
      course.hyperskillProject!!.title = remoteCourse.hyperskillProject!!.title
      course.hyperskillProject!!.description = remoteCourse.hyperskillProject!!.description
    }
  }

  private fun updateProblems(problemsUpdates: List<TaskUpdate>) {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      problemsUpdates.forEach {
        val localTask = it.localTask
        val taskFromServer = it.taskFromServer
        if (localTask.status != CheckStatus.Solved) {
          // if name of remote task changes name of dir local task will not
          GeneratorUtils.createTaskContent(project, taskFromServer, localTask.getDir(project.courseDir)!!)
        }
        updateTaskDescription(localTask, taskFromServer)
        localTask.updateDate = taskFromServer.updateDate
        if (localTask is RemoteEduTask && taskFromServer is RemoteEduTask) {
          localTask.checkProfile = taskFromServer.checkProfile
        }
        YamlFormatSynchronizer.saveItemWithRemoteInfo(localTask)
      }
    }
  }

  private fun updateProjectLesson(remoteCourse: HyperskillCourse) {
    val lesson = course.getProjectLesson() ?: return
    val remoteLesson = remoteCourse.getProjectLesson() ?: return

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
        GeneratorUtils.createChildFile(project, courseDir, additionalFile.name, additionalFile.text)
      }
    }
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
          taskFile.text = remoteTaskFile.text
          taskFile
        }
        else {
          task.addTaskFile(remoteTaskFile)
          remoteTaskFile
        }

        if (updateInLocalFS) {
          val taskDir = task.getDir(project.courseDir)
          if (taskDir != null) {
            GeneratorUtils.createChildFile(project, taskDir, path, currentTaskFile.text)
          }
        }
      }
      task.init(lesson, false)
    }

    val flm = FrameworkLessonManager.getInstance(project)

    if (lesson.currentTaskIndex != task.index - 1) {
      updateTaskFiles(task, remoteTask.testFiles, false)
      flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.text })
    }
    else {
      // With current logic of next/prev action for hyperskill tasks
      // update of non-test files makes sense only for first task
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

    private fun needUpdateCourseAdditionalFiles(project: Project, remoteFiles: List<EduFile>): Boolean {
      val courseDir = project.courseDir
      for (remoteFile in remoteFiles) {
        val needToUpdate = invokeAndWaitIfNeeded {
          runWriteAction {
            if (project.isDisposed) return@runWriteAction false
            val file = courseDir.findFileByRelativePath(remoteFile.name) ?: return@runWriteAction true
            val text = try {
              file.loadEncodedContent()
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

