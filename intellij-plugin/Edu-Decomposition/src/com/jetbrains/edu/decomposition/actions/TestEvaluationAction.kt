package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionDependenciesParser
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.decomposition.test.TestEvaluator
import com.jetbrains.edu.decomposition.test.TestManager
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.asJava.classes.runReadAction
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("ComponentNotRegistered")
class TestEvaluationAction : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(EduDecompositionBundle.message("action.test.evaluation.in.progress"))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    TestEvaluationActionState.getScope(project).launch {
      if (TestEvaluationActionState.getInstance(project).doLock()) {
        try {
          processStarted()
          TaskToolWindowView.getInstance(project).checkStarted(task, false)
          performTaskTesting(project, task)
        } finally {
          TestEvaluationActionState.getInstance(project).unlock()
          processFinished()
        }
      } else {
        e.dataContext.showPopup(EduDecompositionBundle.message("action.test.evaluation.already.running"))
      }
    }
  }

  private suspend fun performTaskTesting(project: Project, task: Task) {
    withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.test.evaluation"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionNames = runReadAction { FunctionParser.extractFunctionNames(files, project, language) }
      val testManager = TestManager.getInstance(project)
      if (!testManager.isTestGenerated(task.id, functionNames)) return@withBackgroundProgress // TODO("There are no generated tests")
      val generatedDependencies = testManager.getTest(task.id) ?: return@withBackgroundProgress
      val dependencies = runReadAction { FunctionDependenciesParser.extractFunctionDependencies(files, project, language) }

      // TODO("Call to test evaluator and handle result")
      val checkResult = TestEvaluator.evaluate(generatedDependencies, dependencies)
      task.status = checkResult.status
      task.feedback = CheckFeedback(Date(), checkResult)
      TaskToolWindowView.getInstance(project).apply {
        checkFinished(task, checkResult)
        updateCheckPanel(task)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    project.selectedTaskFile ?: return
    e.presentation.isEnabledAndVisible = !TestEvaluationActionState.getInstance(project).isLocked
    templatePresentation.text = EduDecompositionBundle.message("action.Educational.Test.Evaluation.text")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  @Service(Service.Level.PROJECT)
  private class TestEvaluationActionState(private val scope: CoroutineScope) {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean = isBusy.compareAndSet(false, true)

    val isLocked: Boolean
      get() = isBusy.get()

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): TestEvaluationActionState = project.service()

      fun getScope(project: Project) = getInstance(project).scope
    }
  }
}