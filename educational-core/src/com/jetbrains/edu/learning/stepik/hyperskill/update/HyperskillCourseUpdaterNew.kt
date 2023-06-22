package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.eduEnvironment
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter
import com.jetbrains.edu.learning.update.EduCourseUpdater
import com.jetbrains.edu.learning.update.UpdateUtils
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException

@Suppress("DuplicatedCode")
class HyperskillCourseUpdaterNew(project: Project, override val course: HyperskillCourse) : EduCourseUpdater(project, course) {
  private var courseFromServer: HyperskillCourse? = null
  private var tasksInFrameworkLessonUpdates: List<TaskUpdate> = emptyList()
  private var problemsUpdates: List<TaskUpdate> = emptyList()

  override val updateAutomatically: Boolean
    get() = HyperskillSettings.INSTANCE.updateAutomatically

  @Synchronized
  override fun areUpdatesAvailable(): Boolean {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    val hyperskillProject = course.hyperskillProject
    val projectShouldBeUpdated = hyperskillProject != null && hyperskillProject.shouldBeUpdated(courseFromServer?.hyperskillProject)

    courseFromServer = getCourseFromServer()
    tasksInFrameworkLessonUpdates = getTasksInFrameworkLessonUpdates()
    problemsUpdates = getProblemsUpdate()
    return projectShouldBeUpdated || tasksInFrameworkLessonUpdates.isNotEmpty() || problemsUpdates.isNotEmpty()
  }

  override fun getCourseFromServer(): HyperskillCourse? {
    val connector = HyperskillConnector.getInstance()
    val hyperskillProject = when (val response = connector.getProject(course.id)) {
      is Err -> return null
      is Ok -> response.value
    }
    val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillProject.language) ?: return null
    val eduEnvironment = hyperskillProject.eduEnvironment ?: return null
    val stagesFromServer = connector.getStages(course.id) ?: return null
    return HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment).apply {
      stages = stagesFromServer
      val lessonFromServer = connector.getLesson(this, hyperskillProject.ideFiles)
      addLesson(lessonFromServer)
      init(this, false)
    }
  }

  @Synchronized
  override fun doUpdate() {
    if (courseFromServer == null && problemsUpdates.isEmpty()) {
      LOG.warn("Nothing to update")
      return
    }

    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    if (courseFromServer != null) {
      updateCourse()
      updateProjectLesson()
    }
    updateProblems()
    TODO()
  }

  private fun updateCourse() {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      val remoteCourse = courseFromServer ?: return@invokeAndWaitIfNeeded
      course.description = remoteCourse.description

      val remoteHyperskillProject = remoteCourse.hyperskillProject ?: return@invokeAndWaitIfNeeded
      course.hyperskillProject?.apply {
        title = remoteHyperskillProject.title
        description = remoteHyperskillProject.description
      }

      courseFromServer?.additionalFiles?.forEach { additionalFile ->
        GeneratorUtils.createChildFile(project, project.courseDir, additionalFile.name, additionalFile.text)
      }
    }
  }

  private fun updateProjectLesson() {
    val lesson = course.getProjectLesson() ?: return
    val remoteLesson = courseFromServer?.getProjectLesson() ?: return

    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      for ((task, remoteTask) in lesson.taskList.zip(remoteLesson.taskList)) {
        if (!task.updateDate.before(remoteTask.updateDate)) continue

        if (task.status != CheckStatus.Solved) {
          // With current logic of next/prev action for hyperskill tasks
          // update of non-test files makes sense only for first task
          UpdateUtils.updateFrameworkLessonFiles(project, lesson, task, remoteTask, task.index == 1)
        }

        UpdateUtils.updateTaskDescription(project, task, remoteTask)
        task.updateDate = remoteTask.updateDate
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }

      courseFromServer?.additionalFiles?.forEach { additionalFile ->
        GeneratorUtils.createChildFile(project, project.courseDir, additionalFile.name, additionalFile.text)
      }
    }
  }

  private fun updateProblems() {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      problemsUpdates.forEach {
        HyperskillTaskUpdater(project).doUpdate(it)
      }
    }
  }

  private fun HyperskillProject.shouldBeUpdated(hyperskillProject: HyperskillProject?): Boolean {
    return when {
      hyperskillProject != null && title != hyperskillProject.title -> true
      else -> false
    }
  }

  private fun needUpdateCourseAdditionalFiles(remoteCourse: HyperskillCourse): Boolean {
    val remoteFiles = remoteCourse.additionalFiles
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

  private fun getTasksInFrameworkLessonUpdates(): List<TaskUpdate> {
    val projectLesson = course.getProjectLesson() ?: return emptyList()
    val localTasks = projectLesson.taskList
    val tasksFromServer = courseFromServer?.getProjectLesson()?.taskList ?: return emptyList()
    val result = localTasks.zip(tasksFromServer).mapNotNull { (local, remote) ->
      if (local.id != remote.id || remote.updateDate.isSignificantlyAfter(local.updateDate)) {
        TaskUpdate(local, remote)
      }
      else {
        null
      }
    }
    return getListOfTaskUpdate(localTasks, tasksFromServer)
  }

  private fun getProblemsUpdate(): List<TaskUpdate> {
    val legacyProblemLesson = course.getProblemsLesson()
    val newProblemLessons = course.getTopicsSection()?.lessons ?: emptyList()
    val problemLessons = listOfNotNull(legacyProblemLesson, *newProblemLessons.toTypedArray())
    return problemLessons.flatMap { lesson -> lesson.getProblemsUpdates() }
  }

  private fun Lesson.getProblemsUpdates(): List<TaskUpdate> {
    val tasksFromServer = HyperskillConnector.getInstance().getProblems(course, this, taskList.map { it.id })
    val localTasks = taskList.associateBy { it.id }

    val result = mutableListOf<TaskUpdate>()
    for (serverTask in tasksFromServer) {
      val localTask = localTasks[serverTask.id]
      if (localTask != null) {
        val localTaskIsExpired = serverTask.updateDate.isSignificantlyAfter(localTask.updateDate)
        val serverTaskIsDifferent = taskIsDifferent(localTask, serverTask)
        serverTask.parent = localTask.lesson
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
    val result = first.descriptionText != second.descriptionText ||
                 first.feedbackLink != second.feedbackLink ||
                 first.name != second.name
    if (result) return true

    return when {
      first is UnsupportedTask && second !is UnsupportedTask -> true
      first is ChoiceTask && second is ChoiceTask -> {
        first.choiceOptions != second.choiceOptions
      }

      first is SortingTask && second is SortingTask -> {
        first.options != second.options
      }

      first is MatchingTask && second is MatchingTask -> {
        (first.options != second.options) || (first.captions != second.captions)
      }

      first is RemoteEduTask && second is RemoteEduTask -> {
        first.checkProfile != second.checkProfile
      }

      else -> false
    }
  }
}