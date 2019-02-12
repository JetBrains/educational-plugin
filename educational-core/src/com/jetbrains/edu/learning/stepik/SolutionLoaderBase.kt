package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import com.jetbrains.edu.learning.update.UpdateNotification
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

abstract class SolutionLoaderBase(protected val project: Project) : Disposable {

  private val futures: MutableMap<Int, Future<Boolean>> = HashMap()

  fun loadSolutionsInBackground() {
    ProgressManager.getInstance().run(object : Backgroundable(project, "Getting Tasks to Update") {
      override fun run(progressIndicator: ProgressIndicator) {
        val course = StudyTaskManager.getInstance(myProject).course
        if (course != null) {
          loadAndApplySolutions(course, progressIndicator)
        }
      }
    })
  }

  @VisibleForTesting
  @JvmOverloads
  fun loadAndApplySolutions(course: Course, progressIndicator: ProgressIndicator? = null) {
    val tasksToUpdate = EduUtils.execCancelable { provideTasksToUpdate(course) }
    if (tasksToUpdate != null) {
      updateTasks(tasksToUpdate, progressIndicator)
    }
    else {
      LOG.warn("Can't get a list of tasks to update")
    }
  }

  private fun updateTasks(tasks: List<Task>, progressIndicator: ProgressIndicator?) {
    cancelUnfinishedTasks()

    val connection = ApplicationManager.getApplication().messageBus.connect()
    // It's supposed to use `selectedTask` only in EDT
    // so it doesn't require additional synchronization
    var selectedTask: Task? = EduUtils.getSelectedEduEditor(project)?.taskFile?.task
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val eduEditor = EduUtils.getSelectedEduEditor(project) ?: return
        val task = eduEditor.taskFile.task
        selectedTask = task
        val future = futures[task.stepId] ?: return
        eduEditor.startLoading()
        enableEditorWhenFutureDone(task, future)
      }
    })

    val tasksToUpdate = tasks.filter { task -> task !is TheoryTask }

    val countDownLatch = CountDownLatch(tasksToUpdate.size)
    for ((i, task) in tasksToUpdate.withIndex()) {
      val progressIndex = i + 1
      if (progressIndicator == null || !progressIndicator.isCanceled) {
        futures[task.stepId] = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
          try {
            if (progressIndicator != null) {
              progressIndicator.fraction = progressIndex.toDouble() / tasksToUpdate.size
              progressIndicator.text = String.format("Loading solution %d of %d", progressIndex, tasksToUpdate.size)
            }
            updateTask(project, task)
          }
          finally {
            countDownLatch.countDown()
          }
        }
      }
      else {
        countDownLatch.countDown()
      }
    }

    runInEdt {
      @Suppress("NAME_SHADOWING")
      val selectedTask = selectedTask ?: return@runInEdt
      val future = futures[selectedTask.stepId] ?: return@runInEdt
      val selectedEduEditor = EduUtils.getSelectedEduEditor(project) ?: return@runInEdt
      selectedEduEditor.startLoading()
      enableEditorWhenFutureDone(selectedTask, future)
    }

    try {
      countDownLatch.await()
      val needToShowNotification = needToShowUpdateNotification()
      runInEdt {
        if (needToShowNotification) {
          UpdateNotification(NOTIFICATION_TITLE, NOTIFICATION_CONTENT).notify(project)
        }
        EduUtils.synchronize()
        selectedTask?.let { updateUI(project, it) }
      }
    }
    catch (e: InterruptedException) {
      LOG.warn(e)
    }
    finally {
      connection.disconnect()
    }
  }

  private fun needToShowUpdateNotification(): Boolean {
    return futures.values.any { future ->
      try {
        val result = future.get()
        result == true
      }
      catch (e: InterruptedException) {
        LOG.warn(e)
        false
      }
      catch (e: ExecutionException) {
        LOG.warn(e)
        false
      }
    }
  }

  private fun cancelUnfinishedTasks() {
    for (future in futures.values) {
      if (!future.isDone) {
        future.cancel(true)
      }
    }
    futures.clear()
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  private fun updateTask(project: Project, task: Task): Boolean {
    val taskSolutions = loadSolution(task)
    if (!taskSolutions.hasIncompatibleSolutions && !taskSolutions.solutions.isEmpty()) {
      applySolutions(project, task, taskSolutions)
    }
    return taskSolutions.hasIncompatibleSolutions
  }

  protected open fun loadSolution(task: Task): TaskSolutions {
    val language = task.course.languageID
    val lastSubmission = loadLastSubmission(task.stepId)
    val reply = lastSubmission?.reply
    val solution = reply?.solution
    if (solution == null || solution.isEmpty()) {
      // https://youtrack.jetbrains.com/issue/EDU-1449
      if (reply != null && reply.solution == null) {
        LOG.warn(String.format("`solution` field of reply object is null for task `%s`", task.name))
      }
      return TaskSolutions.EMPTY
    }

    if (reply.version > JSON_FORMAT_VERSION) {
      // TODO: show notification with suggestion to update plugin
      LOG.warn(String.format("The plugin supports versions of submission reply not greater than %d. The current version is `%d`",
                             JSON_FORMAT_VERSION, reply.version))
      return TaskSolutions.INCOMPATIBLE
    }


    val serializedTask = reply.eduTask ?: return TaskSolutions.EMPTY

    val module = SimpleModule()
    module.addDeserializer(Task::class.java, JacksonSubmissionDeserializer(reply.version, language))
    val objectMapper = StepikConnector.objectMapper.copy()
    objectMapper.registerModule(module)
    val updatedTaskData = try {
      objectMapper.readValue(serializedTask, TaskData::class.java)
    }
    catch (e: IOException) {
      LOG.error(e.message)
      return TaskSolutions.EMPTY
    }

    return TaskSolutions.from(lastSubmission.status ?: "", updatedTaskData.task, reply.solution.orEmpty())
  }

  private fun enableEditorWhenFutureDone(selectedTask: Task, future: Future<*>) {
    if (future.isDone || future.isCancelled) return
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        future.get()
        runInEdt {
          val selectedEditor = EduUtils.getSelectedEduEditor(project) ?: return@runInEdt
          if (selectedEditor.taskFile.name in selectedTask.taskFiles) {
            selectedEditor.stopLoading()
            selectedEditor.validateTaskFile()
          }
        }
      }
      catch (e: InterruptedException) {
        LOG.warn(e)
      }
      catch (e: ExecutionException) {
        LOG.warn(e)
      }
    }
  }

  private fun updateUI(project: Project, task: Task) {
    ProjectView.getInstance(project).refresh()
    // TODO: do we really need it here?
    TaskDescriptionView.getInstance(project).currentTask = task
    NavigationUtils.navigateToTask(project, task)
  }

  override fun dispose() {
    cancelUnfinishedTasks()
  }

  protected abstract fun loadLastSubmission(stepId: Int): Submission?
  protected abstract fun provideTasksToUpdate(course: Course): List<Task>

  companion object {

    private val LOG = Logger.getInstance(SolutionLoaderBase::class.java)

    private const val NOTIFICATION_TITLE = "Outdated EduTools Plugin"
    private const val NOTIFICATION_CONTENT = "<html>Your version of EduTools plugin is outdated to apply all solutions.\n" + "<a href=\"\">Update plugin</a> to avoid compatibility problems.\n"

    private fun updatePlaceholders(taskFile: TaskFile, updatedPlaceholders: List<AnswerPlaceholder>) {
      val answerPlaceholders = taskFile.answerPlaceholders
      if (answerPlaceholders.size != updatedPlaceholders.size) {
        LOG.warn("")
        return
      }
      for ((answerPlaceholder, updatedPlaceholder) in answerPlaceholders.zip(updatedPlaceholders)) {
        answerPlaceholder.placeholderText = updatedPlaceholder.placeholderText
        answerPlaceholder.status = updatedPlaceholder.status
        answerPlaceholder.offset = updatedPlaceholder.offset
        answerPlaceholder.length = updatedPlaceholder.length
        answerPlaceholder.selected = updatedPlaceholder.selected
      }
    }

    private fun applySolutions(project: Project, task: Task, taskSolutions: TaskSolutions) {
      runInEdt {
        val taskDir = task.getTaskDir(project) ?: return@runInEdt
        task.status = taskSolutions.checkStatus
        val solutionsMap = taskSolutions.solutions.mapValues { it.value.first }
        val lesson = task.lesson
        if (lesson is FrameworkLesson && lesson.currentTask() != task) {
          val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
          frameworkLessonManager.saveExternalChanges(task, solutionsMap)
        }
        else {
          for (taskFile in task.taskFiles.values) {
            val (solutionText, placeholders) = taskSolutions.solutions[taskFile.name] ?: continue
            val vFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
            updatePlaceholders(taskFile, placeholders)
            try {
              taskFile.isTrackChanges = false
              runWriteAction {
                VfsUtil.saveText(vFile, solutionText)
              }
              SaveAndSyncHandler.getInstance().refreshOpenFiles()
              taskFile.isTrackChanges = true
            }
            catch (e: IOException) {
              LOG.warn(e)
            }
          }
        }
      }
    }
  }

  protected class TaskSolutions @JvmOverloads constructor(
    val checkStatus: CheckStatus,
    val solutions: Map<String, Pair<String, List<AnswerPlaceholder>>> = emptyMap(),
    val hasIncompatibleSolutions: Boolean = false
  ) {
    companion object {

      val EMPTY = TaskSolutions(CheckStatus.Unchecked)
      val INCOMPATIBLE = TaskSolutions(CheckStatus.Unchecked, hasIncompatibleSolutions = true)

      @JvmStatic
      fun from(status: String, task: Task, solutionList: List<SolutionFile>): TaskSolutions {
        val checkStatus = when (status) {
          "wrong" -> CheckStatus.Failed
          "correct" -> CheckStatus.Solved
          else -> CheckStatus.Unchecked
        }

        val solutions = mutableMapOf<String, Pair<String, List<AnswerPlaceholder>>>()
        for (file in solutionList) {
          val taskFile = task.getTaskFile(file.name) ?: continue
          solutions[file.name] = file.text to taskFile.answerPlaceholders
        }

        return TaskSolutions(checkStatus, solutions)
      }
    }
  }
}
