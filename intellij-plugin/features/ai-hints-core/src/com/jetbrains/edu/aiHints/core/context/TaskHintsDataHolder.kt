package com.jetbrains.edu.aiHints.core.context

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly

@Service(Service.Level.PROJECT)
class TaskHintsDataHolder(private val project: Project) : EduTestAware {
  private val data = ConcurrentHashMap<Task, TaskHintData>()

  suspend fun getOrCreate(task: Task): TaskHintData = data.getOrPut(task) {
    TaskHintData(authorSolutionContext = AuthorSolutionContext.create(project, task))
  }

  fun getTaskHintData(task: Task): TaskHintData? = data[task]

  data class TaskHintData(
    var authorSolutionContext: AuthorSolutionContext,
    var taskFilesWithChangedFunctions: Map<String, List<String>>? = null
  )

  companion object {
    fun getInstance(project: Project): TaskHintsDataHolder = project.service()

    val Task.hintData: TaskHintData
      get() {
        val project = project ?: error("No project for task $name")
        // TODO(refactor to get task hint data using coroutines, see EDU-8720)
        @Suppress("RAW_RUN_BLOCKING")
        return runBlocking {
          getInstance(project).getOrCreate(this@hintData)
        }
      }
  }

  @TestOnly
  override fun cleanUpState() {
    data.clear()
  }
}