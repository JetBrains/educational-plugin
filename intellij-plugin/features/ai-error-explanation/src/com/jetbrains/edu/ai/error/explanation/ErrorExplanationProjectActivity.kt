package com.jetbrains.edu.ai.error.explanation

import com.intellij.execution.ExecutionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.ai.error.explanation.listener.ErrorExplanationExecutionListener

class ErrorExplanationProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!isErrorExplanationEnabled(project)) return

    project.messageBus.connect().subscribe(ExecutionManager.EXECUTION_TOPIC, ErrorExplanationExecutionListener(project))
  }
}