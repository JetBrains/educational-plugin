package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.checker.remote.RemoteTaskCheckerManager.remoteCheckerForTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class RetryAction(actionText: Supplier<String>,
                  private val processMessage: String = PROCESS_MESSAGE,
                  private val expectedTaskStatus: CheckStatus = CheckStatus.Failed
) : ActionWithProgressIcon(actionText),
    DumbAware {

  constructor() : this(EduCoreBundle.lazyMessage("action.retry.try.again")) {
    setUpSpinnerPanel(processMessage)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return

    if (!task.isChangedOnFailed || task.status != expectedTaskStatus) {
      return
    }

    if (!RetryActionState.getInstance(project).doLock()) {
      e.dataContext.showPopup(EduCoreBundle.message("action.retry.already.running"))
      return
    }
    val retryTask = RetryTask(project, task, e.dataContext)
    if (retryTask.isHeadless) {
      /**
       *  [CheckAction]:122
       */
      val future = ApplicationManager.getApplication().executeOnPooledThread { ProgressManager.getInstance().run(retryTask) }
      EduUtils.waitAndDispatchInvocationEvents(future)
    }
    else {
      ProgressManager.getInstance().run(retryTask)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    if (task.status != expectedTaskStatus) return
  }

  private inner class RetryTask(project: Project,
                                private val task: Task,
                                private val context: DataContext
  ) : com.intellij.openapi.progress.Task.Backgroundable(project,
                                                        EduCoreBundle.message("action.retry.task.background"), true) {
    private lateinit var result: Result<Boolean, String>

    override fun run(indicator: ProgressIndicator) {
      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
      val remoteChecker = remoteCheckerForTask(project, task) ?: return
      result = remoteChecker.retry(task) ?: return
    }

    override fun onSuccess() {
      when (val res = result) {
        is Ok -> {
          task.status = CheckStatus.Unchecked
          YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
        }
        is Err -> if (!isHeadless) {
          context.showPopup(res.error)
        }
      }
    }

    override fun onFinished() {
      ApplicationManager.getApplication().invokeLater {
        TaskDescriptionView.getInstance(project).updateTaskSpecificPanel()
        TaskDescriptionView.getInstance(project).updateCheckPanel(task)
      }
      processFinished()
      RetryActionState.getInstance(project).unlock();
    }
  }

  @Service
  private class RetryActionState {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): RetryActionState {
        return project.getService(RetryActionState::class.java)
      }
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Retry"

    @NonNls
    private const val PROCESS_MESSAGE: String = "Retry in progress"
  }
}