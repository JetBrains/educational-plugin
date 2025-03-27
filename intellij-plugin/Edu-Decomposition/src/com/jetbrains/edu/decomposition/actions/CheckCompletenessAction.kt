package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.kotlin.asJava.classes.runReadAction

@Suppress("ComponentNotRegistered")
class CheckCompletenessAction : CheckActionBase() {

  init {
    setUpSpinnerPanel(EduDecompositionBundle.message("progress.title.checking.completeness"))
  }

  override val checkFailureMessage: String = EduDecompositionBundle.message("action.check.completeness.failure")

  override val templatePresentationMessage: String = EduDecompositionBundle.message("action.Educational.Check.Completeness.text")

  override val actionAlreadyRunningMessage: String = EduDecompositionBundle.message("action.check.completeness.already.running")

  override val checkingProgressTitle: String = EduDecompositionBundle.message("progress.title.checking.completeness")

  override suspend fun performCheck(project: Project, task: Task): Boolean {
    val language = task.course.languageById ?: return false
    val files = task.taskFiles.values.filter { it.isVisible }

    val functionNames = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }
    if (functionNames.isEmpty()) return false// TODO
    // TODO: request to ml lib

    return if (functionNames.size >= 2) { // TODO: move to success block
      task.decompositionStatus = DecompositionStatus.GRANULARITY_CHECK_NEEDED
      invokeNextAction(ActionManager.getInstance().getAction("Educational.Check.Granularity") as ActionWithProgressIcon, project)
      true
    }
    else {
      false
    }
  }

}
