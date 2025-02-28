package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.feedback.GranularityFeedbackProvider
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task

@Suppress("ComponentNotRegistered")
class CheckGranularityAction : CheckActionBase() {

  override val checkResultMessage: String = EduDecompositionBundle.message("action.check.granularity.success")

  override val templatePresentationMessage: String = EduDecompositionBundle.message("action.Educational.Check.Granularity.text")

  override val actionAlreadyRunningMessage: String = EduDecompositionBundle.message("action.check.granularity.already.running")

  override val spinnerPanelMessage: String = EduDecompositionBundle.message("progress.title.checking.granularity")

  override suspend fun performCheck(project: Project, task: Task): Boolean {
    return withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.checking.granularity"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress false
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionModels = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }

      val complexFunctions = functionModels.filter { it.isComplexFunction() }
      GranularityFeedbackProvider.provideFeedback(complexFunctions, project)

      return@withBackgroundProgress if(complexFunctions.isEmpty()) {
        task.decompositionStatus = DecompositionStatus.DECOMPOSED
        true
      } else false
    }
  }

  fun FunctionModel.isComplexFunction(): Boolean {
    // TODO: Use LLM to check the granularity
    return name.lowercase().contains("complex")
  }
}
