package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionDependenciesParser
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.decomposition.test.TestDependenciesEvaluator
import com.jetbrains.edu.decomposition.test.TestDependenciesManager
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.kotlin.asJava.classes.runReadAction
import kotlin.coroutines.cancellation.CancellationException

@Suppress("ComponentNotRegistered")
class TestEvaluationAction : CheckActionBase() {

  override val spinnerPanelMessage: String = EduDecompositionBundle.message("progress.title.test.evaluation")

  override val actionAlreadyRunningMessage: String = EduDecompositionBundle.message("action.test.evaluation.already.running")

  override var checkResultMessage: String = ""

  override val templatePresentationMessage: String = EduDecompositionBundle.message("action.Educational.Test.Evaluation.text")

  override suspend fun performCheck(project: Project, task: Task): Boolean {
    return withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.test.evaluation"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress false
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionNames = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }.map { it.name }
      val testManager = TestDependenciesManager.getInstance(project)
      if (!testManager.isTestGenerated(task.id, functionNames)) {
        try {
          testManager.waitForTestGeneration(task.id, functionNames)
        } catch (e: CancellationException) {
          return@withBackgroundProgress false
        }
      }
      val generatedDependencies = testManager.getTest(task.id) ?: return@withBackgroundProgress false
      val dependencies = runReadAction { FunctionDependenciesParser.extractFunctionDependencies(files, project, language) }

      // TODO("Call to test evaluator and handle result")

      return@withBackgroundProgress  if (TestDependenciesEvaluator.evaluate(generatedDependencies, dependencies)) {
        checkResultMessage = EduDecompositionBundle.message("action.test.evaluation.success")
        true
      } else {
        checkResultMessage = EduDecompositionBundle.message("action.test.evaluation.failure")
        false
      }
    }
  }
}