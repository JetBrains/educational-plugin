package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.editor.EduEditor
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.update.UpdateNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class SolutionLoaderBase(protected val project: Project) : Disposable {

  private var futures: Map<Int, Future<Boolean>> = HashMap()

  fun loadSolutionsInBackground() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.solutions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, progressIndicator)
      }
    })
  }

  @VisibleForTesting
  @JvmOverloads
  fun loadAndApplySolutions(course: Course, progressIndicator: ProgressIndicator? = null) {
    val tasksToUpdate = provideTasksToUpdate(course)
    val submissions = if (progressIndicator != null)
      ApplicationUtil.runWithCheckCanceled(Callable { loadSubmissions(tasksToUpdate) }, progressIndicator)
    else loadSubmissions(tasksToUpdate)

    if (submissions != null) {
      updateTasks(course, tasksToUpdate, submissions, progressIndicator)
    }
    else {
      LOG.warn("Can't get submissions")
    }
    runReadAction {
      if (project.isDisposed) return@runReadAction
      project.messageBus.syncPublisher(loadingTopic).solutionLoaded(course)
    }
  }

  protected open fun updateTasks(course: Course, tasks: List<Task>, submissions: List<Submission>, progressIndicator: ProgressIndicator?) {
    progressIndicator?.isIndeterminate = false
    cancelUnfinishedTasks()
    val tasksToUpdate = tasks.filter { task -> task !is TheoryTask }
    var finishedTaskCount = 0
    val futures = HashMap<Int, Future<Boolean>>(tasks.size)
    LOG.info("Started loading solutions")
    val startTime = System.currentTimeMillis()
    for (task in tasksToUpdate) {
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        for (editor in getOpenTaskEditors(project, task)) {
          editor.startLoading()
        }
      }
      futures[task.id] = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
        try {
          ProgressManager.checkCanceled()
          updateTask(project, task, submissions)
        }
        finally {
          if (progressIndicator != null) {
            synchronized(progressIndicator) {
              finishedTaskCount++
              progressIndicator.fraction = finishedTaskCount.toDouble() / tasksToUpdate.size
              progressIndicator.text = "Loading solution $finishedTaskCount of ${tasksToUpdate.size}"
            }
          }
          invokeAndWaitIfNeeded {
            if (project.isDisposed) return@invokeAndWaitIfNeeded
            for (editor in getOpenTaskEditors(project, task)) {
              editor.stopLoading()
              editor.validateTaskFile()
            }
          }
        }
      }
    }

    synchronized(this) {
      this.futures = futures
    }

    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val task = EduUtils.getTaskForFile(project, file) ?: return
        val future = futures[task.id] ?: return
        if (!future.isDone) {
          (source.getSelectedEditor(file) as? EduEditor)?.startLoading()
        }
      }
    })

    try {
      waitAllTasks(futures.values)
      LOG.info("Finished loading solutions (${System.currentTimeMillis() - startTime} ms)")
    }
    finally {
      connection.disconnect()
    }

    val needToShowNotification = needToShowUpdateNotification(futures.values)
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (needToShowNotification) {
        UpdateNotification(NOTIFICATION_TITLE, NOTIFICATION_CONTENT).notify(project)
      }
      EduUtils.synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun getOpenTaskEditors(project: Project, task: Task): List<EduEditor> {
    return FileEditorManager.getInstance(project)
      .allEditors
      .filterIsInstance<EduEditor>()
      .filter { it.taskFile.task == task }
  }

  private fun waitAllTasks(tasks: Collection<Future<*>>) {
    for (task in tasks) {
      if (isUnitTestMode) {
        EduUtils.waitAndDispatchInvocationEvents(task)
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
  private fun updateTask(project: Project, task: Task, submissions: List<Submission>): Boolean {
    val taskSolutions = loadSolution(task, submissions)
    ProgressManager.checkCanceled()
    if (!taskSolutions.hasIncompatibleSolutions && taskSolutions.solutions.isNotEmpty()) {
      applySolutions(project, task, taskSolutions)
    }
    return taskSolutions.hasIncompatibleSolutions
  }

  override fun dispose() {
    cancelUnfinishedTasks()
  }

  protected abstract val loadingTopic: Topic<SolutionLoadingListener>
  protected abstract fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions
  protected abstract fun loadSubmissions(tasks: List<Task>): List<Submission>?
  protected abstract fun provideTasksToUpdate(course: Course): List<Task>

  interface SolutionLoadingListener {
    fun solutionLoaded(course: Course)
  }

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
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        val taskDir = task.getTaskDir(project) ?: return@invokeAndWaitIfNeeded
        task.status = taskSolutions.checkStatus
        YamlFormatSynchronizer.saveItem(task)
        val solutionsMap = taskSolutions.solutions.mapValues { it.value.first }
        val lesson = task.lesson
        if (lesson is FrameworkLesson && lesson.currentTask() != task) {
          val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
          frameworkLessonManager.saveExternalChanges(task, solutionsMap)
          for (taskFile in task.taskFiles.values) {
            val placeholders = taskSolutions.solutions[taskFile.name]?.second ?: continue
            updatePlaceholders(taskFile, placeholders)
          }
        }
        else {
          for (taskFile in task.taskFiles.values) {
            val (solutionText, placeholders) = taskSolutions.solutions[taskFile.name] ?: continue
            val vFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
            if (EduUtils.isTestsFile(project, vFile)) continue
            updatePlaceholders(taskFile, placeholders)
            try {
              taskFile.isTrackChanges = false
              runWriteAction {
                VfsUtil.saveText(vFile, solutionText)
              }
              SaveAndSyncHandler.getInstance().refreshOpenFiles()
            }
            catch (e: IOException) {
              LOG.warn(e)
            }
            finally {
              taskFile.isTrackChanges = true
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

      fun withEmptyPlaceholders(checkStatus: CheckStatus, taskFiles: Map<String, String>): TaskSolutions {
        return TaskSolutions(checkStatus, taskFiles.mapValues {
          @Suppress("RemoveExplicitTypeArguments") // type required by compiler
          it.value to emptyList<AnswerPlaceholder>()
        })
      }
    }
  }
}
