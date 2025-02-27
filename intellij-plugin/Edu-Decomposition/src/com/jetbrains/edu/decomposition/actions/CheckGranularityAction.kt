package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.feedback.GranularityFeedbackProvider
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CheckGranularityAction : CheckActionBase() {

  override fun getActionName(): String = EduDecompositionBundle.message("action.Educational.Check.Completeness.text")

  override suspend fun performCheck(project: Project, task: Task) {
    withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.checking.completeness"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionModels = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }
      GranularityFeedbackProvider.provideFeedback(functionModels.filter { it.isComplexFunction() }, project, language)
    }
  }

  fun FunctionModel.isComplexFunction(): Boolean {
    // TODO: Use LLM to check the granularity
    return name.lowercase().contains("complex")
  }
}
