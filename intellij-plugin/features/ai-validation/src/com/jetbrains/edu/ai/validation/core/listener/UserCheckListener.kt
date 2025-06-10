package com.jetbrains.edu.ai.validation.core.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.collectTestInfo
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.toNameTextMap
import com.jetbrains.edu.ai.validation.core.model.UserResult
import com.jetbrains.edu.ai.validation.core.service.UserResultsService
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class UserCheckListener : CheckListener {
  private val finished = MutableStateFlow(false)

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val userSolution = task.taskFiles.values.filter { !it.isTestFile }.toNameTextMap(project)
    try {
      UserResultsService.getInstance(project).addResult(
        UserResult(
          task = task,
          userSolution = userSolution,
          testInfo = result.collectTestInfo(project, task)
        )
      )
    } finally {
      finished.value = true
    }
  }

  fun clear() {
    finished.value = false
  }

  suspend fun wait() {
    finished.first { it }
  }
}