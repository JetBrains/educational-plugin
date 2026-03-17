package com.jetbrains.edu.learning.ai.completion

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.StudyTaskManager

class AiCompletionProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    updateAiCompletion(project, course)
  }
}
