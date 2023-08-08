package com.jetbrains.edu.learning

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduUtilsKt.isNewlyCreated
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.hasSolutions
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter
import com.jetbrains.edu.learning.update.UpdateNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*
import java.util.Collections.max
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.math.max

abstract class SolutionLoaderBase(protected val project: Project) : Disposable {

  private var futures: Map<Int, Future<Boolean>> = HashMap()

  open fun loadSolutionsInBackground() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.submissions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, progressIndicator)
      }
    })
  }

  fun loadSolutionsInBackground(course: Course, tasksToUpdate: List<Task>, force: Boolean) {
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.submissions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, tasksToUpdate, progressIndicator, force)
      }
    })
  }

  @VisibleForTesting
  fun loadAndApplySolutions(course: Course, progressIndicator: ProgressIndicator? = null) {
    loadAndApplySolutions(course, course.allTasks, progressIndicator)
  }

  private fun loadAndApplySolutions(
    course: Course,
    tasksToUpdate: List<Task>,
    progressIndicator: ProgressIndicator?,
    force: Boolean = false
  ) {
    val submissions = if (progressIndicator != null) {
      ApplicationUtil.runWithCheckCanceled(Callable { loadSubmissions(tasksToUpdate) }, progressIndicator)
    }
    else {
      loadSubmissions(tasksToUpdate)
    }

    if (submissions != null) {
      progressIndicator?.text = EduCoreBundle.message("update.updating.tasks")
      updateTasks(course, tasksToUpdate, submissions, progressIndicator, force)
    }
    else {
      LOG.warn("Can't get submissions")
    }
  }

  protected open fun updateTasks(
    course: Course,
    tasks: List<Task>,
    submissions: List<Submission>,
    progressIndicator: ProgressIndicator?,
    force: Boolean = false
  ) {
    progressIndicator?.isIndeterminate = false
    cancelUnfinishedTasks()
    val tasksToUpdate = tasks.filter { task -> task.hasSolutions() }
    var finishedTaskCount = 0
    val futures = HashMap<Int, Future<Boolean>>(tasks.size)
    for (task in tasksToUpdate) {
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        for (file in getOpenFiles(project, task)) {
          file.startLoading(project)
        }
      }
      futures[task.id] = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
        try {
          ProgressManager.checkCanceled()
          updateTask(project, task, submissions, force)
        }
        finally {
          if (progressIndicator != null) {
            synchronized(progressIndicator) {
              finishedTaskCount++
              progressIndicator.fraction = finishedTaskCount.toDouble() / tasksToUpdate.size
              progressIndicator.text = EduCoreBundle.message("loading.solution.progress", finishedTaskCount, tasksToUpdate.size)
            }
          }
          project.invokeLater {
            for (file in getOpenFiles(project, task)) {
              file.stopLoading(project)
              EditorNotifications.getInstance(project).updateNotifications(file)
            }
          }
        }
      }
    }

    synchronized(this) {
      this.futures = futures
    }

    ProgressManager.checkCanceled()
    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val task = file.getContainingTask(project) ?: return
        val future = futures[task.id] ?: return
        if (!future.isDone) {
          file.startLoading(project)
        }
      }
    })

    try {
      waitAllTasks(futures.values)
    }
    finally {
      connection.disconnect()
    }

    val needToShowNotification = needToShowUpdateNotification(futures.values)
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (needToShowNotification) {
        // Suppression is needed here because DialogTitleCapitalization is demanded by the superclass constructor,
        // but the plugin naming with the capital letters used in the notification title
        @Suppress("DialogTitleCapitalization")
        UpdateNotification(
          EduCoreBundle.message("notification.update.plugin.title"),
          EduCoreBundle.message("notification.update.plugin.apply.solutions.content")
        ).notify(project)
      }
      EduUtilsKt.synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun getOpenFiles(project: Project, task: Task): List<VirtualFile> {
    return FileEditorManager.getInstance(project).openFiles.filter {
      it.getTaskFile(project)?.task == task
    }
  }

  private fun waitAllTasks(tasks: Collection<Future<*>>) {
    for (task in tasks) {
      if (isUnitTestMode) {
        EduActionUtils.waitAndDispatchInvocationEvents(task)
      }
      else {
        try {
          task.get()
        }
        catch (e: Exception) {
          LOG.warn(e)
        }
      }
    }
  }

  private fun needToShowUpdateNotification(tasks: Collection<Future<*>>): Boolean {
    return tasks.any { future ->
      try {
        future.get() == true
      }
      catch (e: Exception) {
        LOG.warn(e)
        false
      }
    }
  }

  @Synchronized
  private fun cancelUnfinishedTasks() {
    for (future in futures.values) {
      if (!future.isDone) {
        future.cancel(true)
      }
    }
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  open fun updateTask(project: Project, task: Task, submissions: List<Submission>, force: Boolean = false): Boolean {
    val taskSolutions = loadSolution(task, submissions)
    ProgressManager.checkCanceled()
    if (task is TheoryTask && taskSolutions.checkStatus == CheckStatus.Solved) {
      applyCheckStatus(task, taskSolutions.checkStatus)
    }
    else if (!taskSolutions.hasIncompatibleSolutions && taskSolutions.solutions.isNotEmpty()) {
      applySolutions(project, task, taskSolutions, force)
    }
    return taskSolutions.hasIncompatibleSolutions
  }

  private fun applyCheckStatus(task: TheoryTask, checkStatus: CheckStatus) {
    task.status = checkStatus
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun dispose() {
    cancelUnfinishedTasks()
  }

  private fun loadSubmissions(tasks: List<Task>): List<Submission>? = SubmissionsManager.getInstance(project).getSubmissions(tasks)

  protected abstract fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions

  companion object {

    private val LOG = Logger.getInstance(SolutionLoaderBase::class.java)

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

    private fun Task.modificationDate(project: Project): Date {
      val lesson = lesson
      return if (lesson is FrameworkLesson && lesson.currentTask() != this) {
        val timestamp = FrameworkLessonManager.getInstance(project).getChangesTimestamp(this)
        Date(max(0, timestamp))
      }
      else {
        val taskDir = getDir(project.courseDir) ?: return Date(0)
        Date(max(taskFiles.values.map { it.findTaskFileInDir(taskDir)?.timeStamp ?: 0 }))
      }
    }

    private fun Task.modifiedBefore(project: Project, taskSolutions: TaskSolutions): Boolean {
      val solutionDate = taskSolutions.date ?: return true
      val localTaskModificationDate = modificationDate(project)
      return solutionDate.isSignificantlyAfter(localTaskModificationDate)
    }

    private fun applySolutions(
      project: Project,
      task: Task,
      taskSolutions: TaskSolutions,
      force: Boolean
    ) {
      project.invokeLater {
        task.status = taskSolutions.checkStatus
        YamlFormatSynchronizer.saveItem(task)
        val lesson = task.lesson
        if (task.course.isStudy && lesson is FrameworkLesson && lesson.currentTask() != task) {
          if (force || task.modifiedBefore(project, taskSolutions)) {
            applySolutionToNonCurrentTask(project, task, taskSolutions)
          }
        }
        else {
          if (force || project.isNewlyCreated() || task.modifiedBefore(project, taskSolutions)) {
            applySolutionToCurrentTask(project, task, taskSolutions)
          }
        }
      }
    }

    private fun applySolutionToNonCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

      frameworkLessonManager.saveExternalChanges(task, taskSolutions.solutions.mapValues { it.value.text })
      for (taskFile in task.taskFiles.values) {
        val solution = taskSolutions.solutions[taskFile.name] ?: continue
        updatePlaceholders(taskFile, solution.placeholders)
        taskFile.isVisible = solution.isVisible
      }
    }

    private fun applySolutionToCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val taskDir = task.getDir(project.courseDir) ?: error("Directory for task `${task.name}` not found")
      for ((path, solution) in taskSolutions.solutions) {
        val taskFile = task.getTaskFile(path)
        if (taskFile == null) {
          GeneratorUtils.createChildFile(project, taskDir, path, solution.text)
          val createdFile = task.getTaskFile(path)
          if (createdFile == null) {
            val help = if (isUnitTestMode) "Don't you forget to use `withVirtualFileListener`?" else ""
            LOG.error("taskFile $path should be created moment ago. $help")
            continue
          }
          createdFile.isVisible = solution.isVisible
        }
        else {
          val vFile = taskDir.findFileByRelativePath(path) ?: continue
          taskFile.isVisible = solution.isVisible

          if (!taskFile.isVisible) continue
          updatePlaceholders(taskFile, solution.placeholders)
          EduDocumentListener.modifyWithoutListener(task, path) {
            runUndoTransparentWriteAction {
              val document = FileDocumentManager.getInstance().getDocument(vFile) ?: error("No document for ${path}")
              document.setText(solution.text)
            }
          }
        }
      }
    }
  }

  protected data class Solution(val text: String, val isVisible: Boolean, val placeholders: List<AnswerPlaceholder>)

  protected class TaskSolutions(
    val date: Date?,
    val checkStatus: CheckStatus,
    val solutions: Map<String, Solution> = emptyMap(),
    val hasIncompatibleSolutions: Boolean = false
  ) {
    companion object {
      val EMPTY = TaskSolutions(null, CheckStatus.Unchecked)
      val INCOMPATIBLE = TaskSolutions(null, CheckStatus.Unchecked, hasIncompatibleSolutions = true)
    }
  }
}
