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
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.whenFalse
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rider.model.AddProjectCommand
import com.jetbrains.rider.model.RdRemoveItemsCommand
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.model.riderSolutionLifecycle
import com.jetbrains.rider.projectView.indexing.updateIndexRules
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.WorkspaceModelEvents
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.util.idea.runCommandUnderProgress
import com.jetbrains.rider.workspaceModel.WorkspaceUserModelUpdater
import java.io.File


@Service(Service.Level.PROJECT)
class CSharpBackendService(private val project: Project) : Disposable {
  private val csprojMergingQueue = TaskMergingQueue()
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

    WorkspaceModelEvents.getInstance(project).synced.whenFalse(project.lifetime) {
      project.solution.riderSolutionLifecycle.isProjectModelReady.whenFalse(project.lifetime) {
        csprojMergingQueue.deactivate()
        indexingMergingQueue.deactivate()
      }
    }
  }

  fun addTasksCSProjectToSolution(tasks: List<Task>) = csprojMergingQueue.queue(Update.create(AddRequest(tasks)) {})

  fun removeProjectModelEntitiesFromSolution(entities: List<ProjectModelEntity>) =
    csprojMergingQueue.queue(Update.create(RemoveRequest(entities)) {})

  fun removeCSProjectFilesFromSolution(tasks: List<Task>) {
    /*
    Should be done synchronously, since the csproj name depends on the course structure.
    If we do this asynchronously, the course structure will be modified earlier than
    we retrieve the csproj name to unload
    */
    val entities = tasks.mapNotNull { task -> task.toProjectModelEntity(project) }
    val ids = entities.mapNotNull { it.getId(project) }.ifEmpty { return }
    val command = RdRemoveItemsCommand(ids)
    try {
      project.solution.projectModelTasks.remove.runCommandUnderProgress(
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

  fun includeFilesToCourseView(files: List<File>) {
    indexingMergingQueue.queue(Update.create(files) {
      WorkspaceUserModelUpdater.getInstance(project).tryInclude(files)
      updateIndexRules(project)
    })
  }

  fun excludeFilesFromCourseView(files: List<File>) {
    indexingMergingQueue.queue(Update.create(files) {
      WorkspaceUserModelUpdater.getInstance(project).tryExclude(files)
      updateIndexRules(project)
    })
  }

  override fun dispose() {}

  private inner class TaskMergingQueue : MergingUpdateQueue(PROJECT_MODEL_READY, 500, false, null, null, null, true) {
    override fun execute(updates: Array<out Update>) {
      val entities = extract<RemoveRequest>(updates).flatMap { it.entities }
      val ids = entities.mapNotNull { it.getId(project) }
      if (ids.isNotEmpty()) {
        val removeCommand = RdRemoveItemsCommand(ids)
        try {
          project.solution.projectModelTasks.remove.runCommandUnderProgress(
            removeCommand,
            project,
            EduCSharpBundle.message("removing.project.files"),
            isCancelable = true,
            throwFault = true
          )
        }
        catch (e: RdFault) {
          LOG.error("Failed to unload task", e)
        }
      }

      val tasksToAdd = extract<AddRequest>(updates).flatMap { it.tasks }
      val taskPaths = tasksToAdd.map { it.csProjPathByTask(project) }
      val parentId = project.getSolutionEntity()?.getId(project) ?: return
      val parameters = RdPostProcessParameters(false, listOf())
      val addCommand = AddProjectCommand(parentId, taskPaths, listOf(), true, parameters)
      try {
        project.solution.projectModelTasks.addProject.runCommandUnderProgress(
          addCommand,
          project,
          EduCSharpBundle.message("adding.projects.to.solution"),
          isCancelable = false,
          throwFault = true
        )
      }
      catch (e: RdFault) {
        LOG.warn("Could not add csproj files to solution", e)
        if (isUserWantsToRetry()) {
          val tasksToRetry = tasksToAdd.filter { e.message?.contains(it.csProjPathByTask(project)) == true }
          if (tasksToRetry.isNotEmpty()) {
            addTasksCSProjectToSolution(tasksToRetry)
          }
        }
      }
    }

    inline fun <reified T> extract(updates: Array<out Update>): List<T> =
      updates.flatMap { update -> update.equalityObjects.filterIsInstance<T>() }

    @Suppress("HardcodedStringLiteral")
    private fun isUserWantsToRetry(): Boolean = Messages.showOkCancelDialog(
      EduCSharpBundle.getMessage("unexpected.error.occurred.when.adding.task.retry"),
      CommonBundle.getErrorTitle(),
      EduCoreBundle.getMessage("retry"),
      CommonBundle.getCancelButtonText(),
      Messages.getErrorIcon()
    ) == MessageConstants.OK

  }

  private data class AddRequest(val tasks: List<Task>)
  private data class RemoveRequest(val entities: List<ProjectModelEntity>)

  companion object {
    fun getInstance(project: Project): CSharpBackendService = project.service()
    private val LOG = logger<CSharpBackendService>()
    private const val PROJECT_MODEL_READY: String = "Project Model Ready"
    private const val INDEXING_REQUEST: String = "Indexing Request"
  }
}
