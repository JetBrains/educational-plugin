package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewRenderer
import com.intellij.notification.NotificationAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.GotItTooltip
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.execCancelable
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduInformationNotification
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent


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

    val connector = task.getStepikBasedConnector()
    val attemptResult = if (submitNewAttempt) {
      connector.postAttempt(task)
    }
    else {
      connector.getActiveAttemptOrPostNew(task)
    }
    val retrievedAttempt = attemptResult.onError { error ->
      processError(project, "Error getting attempt for task with ${task.id} id: $error")
      return
    }
    ProgressManager.checkCanceled()

    val dataset = execCancelable { connector.getDataset(retrievedAttempt) }?.onError { error ->
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

  @Throws(IOException::class)
  private fun DataTask.getOrCreateDataset(project: Project, input: String): VirtualFile {
    val taskDir = getDir(project.courseDir) ?: error("Unable to find task directory")
    val dataset = runReadAction {
      taskDir.findFileByRelativePath(datasetFilePath)
    }
    if (dataset == null) {
      return GeneratorUtils.createTextChildFile(project, taskDir, datasetFilePath, input) ?: error("File $datasetFilePath can't be created")
    }

    val datasetDocument = runReadAction {
      FileDocumentManager.getInstance().getDocument(dataset)
    } ?: error("Can't get document of dataset file - ${dataset.path}")
    if (datasetDocument.text != input) {
      GeneratorUtils.runInWriteActionAndWait {
        VfsUtil.saveText(dataset, input)
        FileDocumentManager.getInstance().reloadFromDisk(datasetDocument)
      }
    }
    return dataset
  }

  override fun onSuccess() {
    val attempt = attempt ?: return
    val dataset = downloadedDataset ?: return
    val isDatasetNewlyCreated = task.attempt?.id != attempt.id

    task.attempt = attempt.toDataTaskAttempt()
    YamlFormatSynchronizer.saveItemWithRemoteInfo(task)

    showAndOpenDataset(dataset)
    if (isDatasetNewlyCreated) {
      showDatasetFilePathNotification(
        project, EduCoreBundle.message("hyperskill.dataset.downloaded.successfully"),
        dataset.presentableUrl
      )
      if (!isUnitTestMode) {
        showTooltipForDataset(project, dataset)
      }
    }
    TaskToolWindowView.getInstance(project).updateCheckPanel(task)
  }

  private fun showTooltipForDataset(project: Project, virtualFile: VirtualFile) {
    val message = EduCoreBundle.getMessage("hyperskill.dataset.here.is.your.dataset")
    val tooltip = GotItTooltip(TOOLTIP_ID, message, project)
      // Update width if message has been changed
      .withMaxWidth(JBUI.scale(270))
      .withPosition(Balloon.Position.atRight)

    if (tooltip.canShow()) {
      virtualFile.showTooltipInCourseView(project, tooltip)
    }
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

  private fun showDatasetFilePathNotification(
    project: Project,
    @NlsContexts.NotificationTitle message: String,
    @NlsContexts.NotificationContent filePath: String
  ) {
    EduInformationNotification(message, filePath).apply {
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

    EduNotificationManager.showErrorNotification(
      project,
      content = EduCoreBundle.message("hyperskill.download.dataset.failed.to.download.dataset")
    )
  }

  @Service(Service.Level.PROJECT)
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
      fun getInstance(project: Project): DownloadDatasetState = project.service()
    }
  }

  companion object {
    private val LOG = Logger.getInstance(DownloadDataset::class.java)

    @NonNls
    private const val TOOLTIP_ID: String = "downloaded.dataset.file"

    fun isRunning(project: Project): Boolean {
      return DownloadDatasetState.getInstance(project).isLocked
    }

    fun lock(project: Project): Boolean {
      return DownloadDatasetState.getInstance(project).lock()
    }

    private fun VirtualFile.showTooltipInCourseView(project: Project, tooltip: GotItTooltip) {
      if (!tooltip.canShow()) return
      ApplicationManager.getApplication().assertIsDispatchThread()

      val psiElement = document.toPsiFile(project) ?: return
      val projectView = ProjectView.getInstance(project)

      projectView.changeViewCB(CourseViewPane.ID, null).doWhenProcessed {
        projectView.selectCB(psiElement, this, true).doWhenProcessed {
          val tree = projectView.currentProjectViewPane.tree
          val location = JBPopupFactory.getInstance().guessBestPopupLocation(tree)

          val point = when (tooltip.position) {
            Balloon.Position.atRight -> {
              val cellRenderer = tree.cellRenderer as? ProjectViewRenderer
              if (cellRenderer == null) {
                LOG.warn("ProjectView tree CellRenderer is not of type ProjectViewRenderer")
              }

              // icon width + text width
              val xOffset = (cellRenderer?.icon?.iconWidth ?: 0) +
                            (tree.getPathBounds(tree.selectionPath)?.width ?: 0)
              // 1/2 text height
              val yOffset = tree.getPathBounds(tree.selectionPath)?.height?.div(2) ?: 0
              location.point.addXOffset(xOffset).addYOffset(-yOffset)
            }
            else -> {
              LOG.warn("Unsupported position ${tooltip.position}")
              location.point
            }
          }

          val component = (location.component as JComponent)
          tooltip.show(component) { _, _ -> point }
        }
      }
    }

    private fun Point.addXOffset(offset: Int) = Point(x + offset, y)
    private fun Point.addYOffset(offset: Int) = Point(x, y + offset)

    private val datasetFilePath: String = GeneratorUtils.joinPaths(
      DataTask.DATA_FOLDER_NAME,
      DataTask.DATASET_FOLDER_NAME,
      DataTask.INPUT_FILE_NAME
    )
  }
}