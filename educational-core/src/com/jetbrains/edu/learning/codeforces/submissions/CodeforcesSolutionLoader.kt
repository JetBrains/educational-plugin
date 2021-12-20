package com.jetbrains.edu.learning.codeforces.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.submissions.Submission
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class CodeforcesSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  private var myBusConnection: MessageBusConnection? = null
  private var mySelectedTask: Task? = null
  private val myFutures = HashMap<Int, Future<Boolean>>()

  init {
    mySelectedTask = EduUtils.getCurrentTask(project)
    addFileOpenListener()
  }

  fun addFileOpenListener() {
    myBusConnection = ApplicationManager.getApplication().messageBus.connect()
    myBusConnection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (project.isDisposed) return
        val taskFile = file.getTaskFile(project) ?: return
        mySelectedTask = taskFile.task
        val task = taskFile.task
        if (myFutures.containsKey(task.id)) {
          file.startLoading(project)
          val future = myFutures[task.id] ?: return
          if (!future.isDone || !future.isCancelled) {
            enableEditorWhenFutureDone(future)
          }
        }
      }
    })
  }

  private fun enableEditorWhenFutureDone(future: Future<*>) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        future.get()
        ApplicationManager.getApplication().invokeLater {
          val eduState = project.eduState
          val selectedTask = mySelectedTask
          if (eduState != null && selectedTask != null && selectedTask.taskFiles.containsKey(eduState.taskFile.name)) {
            eduState.virtualFile.stopLoading(project)
            EditorNotifications.getInstance(project).updateNotifications(eduState.virtualFile)
          }
        }
      }
      catch (e: InterruptedException) {
        LOG.warn(e.message)
      }
      catch (e: ExecutionException) {
        LOG.warn(e.message)
      }
    }
  }

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    val lastSubmission = submissions.firstOrNull { it.step == task.id }
    val reply = lastSubmission?.reply ?: return TaskSolutions.EMPTY

    val files: Map<String, Solution> = reply.solution
                                         ?.associate { it.name to Solution(it.name, it.isVisible, emptyList()) }
                                         ?.filter { (_, solution) -> solution.isVisible } ?: emptyMap()

    return if (files.isEmpty() ) TaskSolutions.EMPTY
    else TaskSolutions(lastSubmission.time, lastSubmission.status.toCheckStatus(), files)
  }


  private fun String?.toCheckStatus(): CheckStatus = when (this) {
    EduNames.WRONG -> CheckStatus.Failed
    EduNames.CORRECT -> CheckStatus.Solved
    else -> CheckStatus.Unchecked
  }

  override fun provideTasksToUpdate(course: Course): List<Task> {
    return course.items.flatMap {
      when (it) {
        is Lesson -> it.taskList
        else -> emptyList()
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): CodeforcesSolutionLoader = project.getService(CodeforcesSolutionLoader::class.java)

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }
}