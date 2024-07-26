package com.jetbrains.edu.csharp

import com.intellij.CommonBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.whenFalse
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.model.AddProjectCommand
import com.jetbrains.rider.model.RdRemoveItemsCommand
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.model.riderSolutionLifecycle
import com.jetbrains.rider.projectView.indexing.updateIndexRules
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.WorkspaceModelEvents
import com.jetbrains.rider.projectView.workspace.findProjectsByName
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getSolutionEntity
import com.jetbrains.rider.util.idea.runCommandUnderProgress
import com.jetbrains.rider.workspaceModel.WorkspaceUserModelUpdater
import java.io.File


@Service(Service.Level.PROJECT)
class CSharpBackendService(private val project: Project) : Disposable {
  private val csprojMergingQueue = MergingUpdateQueue(PROJECT_MODEL_READY, 300, false, null, this, null, true)
  private val indexingMergingQueue = MergingUpdateQueue(INDEXING_REQUEST, 300, false, null, this)

  init {
    WorkspaceModelEvents.getInstance(project).synced.whenTrue(project.lifetime) {
      project.solution.riderSolutionLifecycle.isProjectModelReady.whenTrue(project.lifetime) {
        csprojMergingQueue.activate()
        indexingMergingQueue.activate()
      }
      project.solution.riderSolutionLifecycle.isProjectModelReady.whenFalse(project.lifetime) {
        csprojMergingQueue.deactivate()
        indexingMergingQueue.deactivate()
      }
    }
  }

  fun addCSProjectFilesToSolution(tasks: List<Task>) {
    csprojMergingQueue.queue(Update.create(tasks) {
      val csprojTaskPaths = tasks.map { it.csProjPathByTask(project).toIOFile().toString() }
      val projectModelTasks = project.solution.projectModelTasks
      val parentId = WorkspaceModel.getInstance(project).getSolutionEntity()?.getId(project) ?: return@create
      val parameters = RdPostProcessParameters(false, listOf())
      val command = AddProjectCommand(parentId, csprojTaskPaths, listOf(), true, parameters)
      try {
        projectModelTasks.addProject.runCommandUnderProgress(
          command,
          project,
          EduCSharpBundle.message("adding.projects.to.solution"),
          isCancelable = false,
          throwFault = true
        )
      }
      catch (e: RdFault) {
        LOG.warn("Could not add csproj files to solution", e)
        if (isUserWantsToRetry()) {
          addCSProjectFilesToSolution(tasks)
        }
      }
    })
  }

  fun removeCSProjectFilesFromSolution(tasks: List<Task>) {
    // Should be done synchronously, since the csproj name depends on the course structure.
    // If we do this asynchronously, the course structure will be modified earlier than
    // we retrieve the csproj name to unload
    val projectModelTasks = project.solution.projectModelTasks
    val entities = tasks.mapNotNull { task ->
      WorkspaceModel.getInstance(project).findProjectsByName(task.getCSProjFileNameWithoutExtension()).firstOrNull()
    }
    val ids = entities.mapNotNull { it.getId(project) }.ifEmpty { return }
    val command = RdRemoveItemsCommand(ids)
    try {
      projectModelTasks.remove.runCommandUnderProgress(
        command,
        project,
        EduCSharpBundle.message("removing.project.files"),
        isCancelable = true
      )
    }
    catch (e: RdFault) {
      LOG.error("Failed to unload task", e)
    }
  }

  @Suppress("HardcodedStringLiteral")
  private fun isUserWantsToRetry(): Boolean = Messages.showOkCancelDialog(
    EduCSharpBundle.getMessage("unexpected.error.occurred.when.adding.task.retry"),
    CommonBundle.getErrorTitle(),
    EduCoreBundle.getMessage("retry"),
    CommonBundle.getCancelButtonText(),
    Messages.getErrorIcon()
  ) == MessageConstants.OK

  fun startIndexingTopLevelFiles(files: List<File>) {
    indexingMergingQueue.queue(Update.create(files) {
      WorkspaceUserModelUpdater.getInstance(project).tryInclude(files)
      updateIndexRules(project)
    })
  }

  fun stopIndexingTopLevelFiles(files: List<File>) {
    indexingMergingQueue.queue(Update.create(files) {
      WorkspaceUserModelUpdater.getInstance(project).tryExclude(files)
      updateIndexRules(project)
    })
  }

  override fun dispose() {}

  companion object {
    fun getInstance(project: Project): CSharpBackendService = project.service()
    private val LOG = logger<CSharpBackendService>()
    private const val PROJECT_MODEL_READY: String = "Project Model Ready"
    private const val INDEXING_REQUEST: String = "Indexing Request"
  }

}
