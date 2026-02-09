package com.jetbrains.edu.csharp

import com.intellij.CommonBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.Cancellation.ensureActive
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.whenFalse
import com.jetbrains.rd.util.reactive.whenTrue
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Service(Service.Level.PROJECT)
class CSharpBackendService(private val project: Project, private val scope: CoroutineScope) : LifetimedService() {
  private val active = MutableStateFlow(false)

  private val csprojRequestChannel = Channel<CsprojRequest>(Channel.UNLIMITED)
  private val indexingRequestChannel = Channel<IndexingRequest>(Channel.UNLIMITED)

  init {
    WorkspaceModelEvents.getInstance(project).synced.whenTrue(serviceLifetime) {
      scope.launch(Dispatchers.EDT) {
        project.solution.riderSolutionLifecycle.isProjectModelReady.whenTrue(serviceLifetime) {
          active.value = true
        }
        project.solution.riderSolutionLifecycle.isProjectModelReady.whenFalse(serviceLifetime) {
          active.value = false
        }
      }
    }

    WorkspaceModelEvents.getInstance(project).synced.whenFalse(serviceLifetime) {
      scope.launch(Dispatchers.EDT) {
        project.solution.riderSolutionLifecycle.isProjectModelReady.whenFalse(serviceLifetime) {
          active.value = false
        }
      }
    }

    startCsprojProcessor()
    startIndexingProcessor()
  }

  fun addTasksCSProjectToSolution(tasks: List<Task>) {
    scope.launch {
      csprojRequestChannel.send(CsprojRequest.Add(tasks))
    }
  }

  fun removeProjectModelEntitiesFromSolution(entities: List<ProjectModelEntity>) {
    scope.launch {
      csprojRequestChannel.send(CsprojRequest.Remove(entities))
    }
  }

  fun includeFilesToCourseView(files: List<File>) {
    scope.launch {
      indexingRequestChannel.send(IndexingRequest.Include(files))
    }
  }

  fun excludeFilesFromCourseView(files: List<File>) {
    scope.launch {
      indexingRequestChannel.send(IndexingRequest.Exclude(files))
    }
  }

  private fun startCsprojProcessor() {
    scope.launch {
      csprojRequestChannel.receiveAsFlow()
        .collect { request ->
          active.first { it } // wait for the project model synchronization
          withContext(Dispatchers.EDT) {
            processCSProjRequest(request)
          }
        }
    }
  }

  private fun startIndexingProcessor() {
    scope.launch {
      indexingRequestChannel.receiveAsFlow()
        .collect { request ->
          active.first { it } // wait for the project model synchronization
          withContext(Dispatchers.EDT) {
            processIndexingRequest(request)
          }
        }
    }
  }

  private fun processCSProjRequest(request: CsprojRequest) {
    ensureActive()

    when (request) {
      is CsprojRequest.Add -> {
        val taskPaths = request.tasks.map { it.csProjPathByTask(project) }
        val parentId = project.getSolutionEntity()?.getId(project) ?: return
        val parameters = RdPostProcessParameters(false, listOf())
        val addCommand = createAddProjectCommand(parentId, taskPaths, parameters)

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
            val tasksToRetry = request.tasks.filter { e.message?.contains(it.csProjPathByTask(project)) == true }
            if (tasksToRetry.isNotEmpty()) {
              addTasksCSProjectToSolution(tasksToRetry)
            }
          }
        }
      }

      is CsprojRequest.Remove -> {
        val ids = request.entities.mapNotNull { it.getId(project) }
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
      }
    }
  }

  private fun processIndexingRequest(request: IndexingRequest) {
    ensureActive()

    when (request) {
      is IndexingRequest.Include -> {
        WorkspaceUserModelUpdater.getInstance(project).tryInclude(request.files)
        updateIndexRules(project)
      }

      is IndexingRequest.Exclude -> {
        WorkspaceUserModelUpdater.getInstance(project).tryExclude(request.files)
        updateIndexRules(project)
      }
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

  private sealed class CsprojRequest {
    data class Add(val tasks: List<Task>) : CsprojRequest()
    data class Remove(val entities: List<ProjectModelEntity>) : CsprojRequest()
  }

  private sealed class IndexingRequest {
    data class Include(val files: List<File>) : IndexingRequest()
    data class Exclude(val files: List<File>) : IndexingRequest()
  }

  companion object {
    fun getInstance(project: Project): CSharpBackendService = project.service()
    private val LOG = logger<CSharpBackendService>()
  }
}