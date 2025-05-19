package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.feedback.GranularityFeedbackProvider
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.educational.ml.decompose.core.SingleResponsibilityAssistant

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

    val functionsResult = SingleResponsibilityAssistant.checkSingleResponsibility(functionModels.map { it.name }).getOrThrow().content.filter {!it.verdict.value} .map{ it.function }
    val complexFunctions = functionModels.filter { functionsResult.contains(it.name) }
    GranularityFeedbackProvider.provideFeedback(complexFunctions, project)

    return if(complexFunctions.isEmpty()) {

      task.decompositionStatus = DecompositionStatus.DEPENDENCIES_CHECK_NEEDED
      invokeNextAction(ActionManager.getInstance().getAction("Educational.Check.Dependencies") as ActionWithProgressIcon, project)
      true
    } else {
      task.decompositionStatus = DecompositionStatus.COMPLETENESS_CHECK_NEEDED
      false
    }
  }

}
