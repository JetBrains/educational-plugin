package com.jetbrains.edu.aiHints.core.context

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.rd.util.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TaskHintsDataHolder {
  val data = ConcurrentHashMap<Task, TaskHintData>()

  fun getOrCreate(project: Project, task: Task): TaskHintData = data.getOrPut(task) {
    TaskHintData(authorSolutionContext = AuthorSolutionContext.create(project, task))
  }

  data class TaskHintData(
    val authorSolutionContext: AuthorSolutionContext,
    val taskFilesWithChangedFunctions: Map<String, List<String>>? = null
  )

  companion object {
    fun getInstance(project: Project): TaskHintsDataHolder = project.service()
  }
}