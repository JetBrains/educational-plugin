package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtils.execCancelable
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class DownloadDataset(
  project: Project,
  private val task: DataTask,
  private val submitNewAttempt: Boolean,
  private val onFinishedCallback: () -> Unit
) : Backgroundable(project, EduCoreBundle.message("hyperskill.action.downloading.dataset"), true) {

  @Volatile
  private var attempt: Attempt? = null

  @Volatile
  private var downloadedDataset: VirtualFile? = null

  override fun run(indicator: ProgressIndicator) {
    if (!DownloadDatasetState.getInstance(project).isLocked) {
      processError(project, "Download dataset task is not locked")
      return
    }

    indicator.isIndeterminate = true

    if (task.id == 0) {
      processError(project, "Task is corrupted, there is no task id")
      return
    }

    val retrievedAttempt = getAttempt(project, task)?.onError { error ->
      processError(project, "Error getting attempt for task with ${task.id} id: $error")
      null
    } ?: return
    ProgressManager.checkCanceled()

    val dataset = execCancelable { HyperskillConnector.getInstance().getDataset(retrievedAttempt.id) }?.onError { error ->
      processError(project, "Error getting dataset for attempt with ${retrievedAttempt.id} id: $error")
      null
    } ?: return
    ProgressManager.checkCanceled()

    attempt = retrievedAttempt
    downloadedDataset = try {
      task.getOrCreateDataset(project, dataset)
    }
    catch (e: IOException) {
      processError(project, exception = e)
      return
    }
  }

  override fun onSuccess() {
    val attempt = attempt ?: return
    val dataset = downloadedDataset ?: return
    val isDatasetNewlyCreated = task.attempt?.id != attempt.id

    task.attempt = attempt.toDataTaskAttempt()
    YamlFormatSynchronizer.saveItemWithRemoteInfo(task)

    showAndOpenDataset(dataset)
    if (isDatasetNewlyCreated) {
      showDatasetFilePathNotification(project, EduCoreBundle.message("hyperskill.dataset.downloaded.successfully"),
                                      dataset.presentableUrl)
    }
    TaskDescriptionView.getInstance(project).updateCheckPanel(task)
  }

  override fun onFinished() {
    DownloadDatasetState.getInstance(project).unlock()
    onFinishedCallback()
  }

  private fun showAndOpenDataset(dataset: VirtualFile) {
    FileEditorManager.getInstance(project).openFile(dataset, false)
    val psiElement = dataset.document.toPsiFile(project) ?: return
    ProjectView.getInstance(project).selectPsiElement(psiElement, true)
  }

  private fun getAttempt(project: Project, task: DataTask): Result<Attempt, String>? {
    return execCancelable {
      if (!submitNewAttempt) {
        val existingActiveAttempt = HyperskillConnector.getInstance().getActiveAttempt(task.id)

        if (existingActiveAttempt is Ok && existingActiveAttempt.value != null) {
          return@execCancelable Ok(existingActiveAttempt.value)
        }
      }

      val newAttempt = HyperskillConnector.getInstance().postAttempt(task.id).onError { error ->
        processError(project, "Unable to create new attempt for task with ${task.id} id: $error")
        return@execCancelable Err(error)
      }
      return@execCancelable Ok(newAttempt)
    }
  }

  @Suppress("UnstableApiUsage")
  private fun showDatasetFilePathNotification(project: Project,
                                              @NlsContexts.NotificationTitle message: String,
                                              @NlsContexts.NotificationContent filePath: String) {
    Notification("EduTools", message, filePath, INFORMATION).apply {
      addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("copy.path.to.clipboard")) {
        CopyPasteManager.getInstance().setContents(StringSelection(filePath))
      })
    }.notify(project)
  }

  private fun processError(project: Project, errorMessage: String? = null, exception: Exception? = null) {
    if (errorMessage != null) {
      LOG.error(errorMessage, exception)
    }
    else if (exception != null) {
      LOG.error(exception)
    }

    showErrorNotification(project)
  }

  private fun showErrorNotification(project: Project) {
    Notification(
      "EduTools",
      "",
      EduCoreBundle.message("hyperskill.download.dataset.failed.to.download.dataset"),
      NotificationType.ERROR
    ).notify(project)
  }

  @Service
  private class DownloadDatasetState {
    private val isBusy = AtomicBoolean(false)

    val isLocked: Boolean
      get() = isBusy.get()

    fun lock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      @JvmStatic
      fun getInstance(project: Project): DownloadDatasetState = project.service()
    }
  }

  companion object {
    private val LOG = Logger.getInstance(DownloadDataset::class.java)

    fun isRunning(project: Project): Boolean {
      return DownloadDatasetState.getInstance(project).isLocked
    }

    fun lock(project: Project): Boolean {
      return DownloadDatasetState.getInstance(project).lock()
    }
  }
}