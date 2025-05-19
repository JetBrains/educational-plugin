package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.educational.ml.decompose.core.DependenciesAssistant
import org.jetbrains.kotlin.asJava.classes.runReadAction

@Suppress("ComponentNotRegistered")
class CheckDependenciesAction : CheckActionBase() {

  init {
    setUpSpinnerPanel(EduDecompositionBundle.message("progress.title.checking.dependencies"))
  }

  override var checkFailureMessage: String = ""

  override val templatePresentationMessage: String = EduDecompositionBundle.message("action.Educational.Check.Dependencies.text")

  override val actionAlreadyRunningMessage: String = EduDecompositionBundle.message("action.check.dependencies.already.running")

  override val checkingProgressTitle: String = EduDecompositionBundle.message("progress.title.checking.dependencies")

  override suspend fun performCheck(project: Project, task: Task): Boolean {
    val language = task.course.languageById ?: return false
    val files = task.taskFiles.values.filter { it.isVisible }

    val functionNames = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }.map { it.name }

    val dependencies = runReadAction { FunctionParser.extractDependencies(files, project, language) }

    val dependenciesListForLLM = dependencies.map { outer -> Pair(outer.first.name, outer.second.map { it.name }) }

    val response = DependenciesAssistant.checkDependencies(task.descriptionText, functionNames, dependenciesListForLLM).getOrThrow()


    return if (response.content.verdict.value) { // TODO: move to success block
      val nextAction = CheckAction(task.getUICheckLabel())
      invokeNextAction(nextAction, project)
      true
    }
    else {
      checkFailureMessage = response.content.feedback
      false
    }
  }

}
