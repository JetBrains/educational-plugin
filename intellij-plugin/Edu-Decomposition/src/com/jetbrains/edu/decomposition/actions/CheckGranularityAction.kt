package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.feedback.GranularityFeedbackProvider
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.getUICheckLabel

@Suppress("ComponentNotRegistered")
class CheckGranularityAction : CheckActionBase() {

  init {
    setUpSpinnerPanel(EduDecompositionBundle.message("progress.title.checking.granularity"))
  }

  override val checkFailureMessage: String = EduDecompositionBundle.message("action.check.granularity.failure")

  override val templatePresentationMessage: String = EduDecompositionBundle.message("action.Educational.Check.Granularity.text")

  override val actionAlreadyRunningMessage: String = EduDecompositionBundle.message("action.check.granularity.already.running")

  override val checkingProgressTitle: String = EduDecompositionBundle.message("progress.title.checking.granularity")

  override suspend fun performCheck(project: Project, task: Task): Boolean {
    val language = task.course.languageById ?: return false
    val files = task.taskFiles.values.filter { it.isVisible }
    val functionModels = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }

    val complexFunctions = functionModels.filter { it.isComplexFunction() }
    GranularityFeedbackProvider.provideFeedback(complexFunctions, project)

    return if(complexFunctions.isEmpty()) {
      task.decompositionStatus = DecompositionStatus.DEPENDENCIES_CHECK_NEEDED
      val nextAction = CheckAction(task.getUICheckLabel()) // TODO: replace by calling dependency checking and test generation
      invokeNextAction(nextAction, project)
      true
    } else {
      false
    }
  }

  private fun FunctionModel.isComplexFunction(): Boolean {
    // TODO: Use LLM to check the granularity
    return name.lowercase().contains("complex")
  }
}
