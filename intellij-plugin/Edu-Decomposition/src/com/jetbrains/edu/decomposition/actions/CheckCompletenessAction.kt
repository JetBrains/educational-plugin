package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.kotlin.asJava.classes.runReadAction
import java.util.*

class CheckCompletenessAction : CheckActionBase() {

  override fun getActionName(): String = EduDecompositionBundle.message("action.Educational.Check.Completeness.text")

  override suspend fun performCheck(project: Project, task: Task) {
    withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.checking.completeness"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionNames = runReadAction { FunctionParser.extractFunctionModels(files, project, language) }
      if (functionNames.isEmpty()) return@withBackgroundProgress // TODO
      // TODO: request to ml lib
      if (functionNames.size >= 2) { // TODO: move to success block
        task.decompositionStatus = DecompositionStatus.GRANULARITY_CHECK_NEEDED
        val checkResult = CheckResult(CheckStatus.Solved, EduDecompositionBundle.message("action.check.completeness.success"))
        task.status = checkResult.status
        task.feedback = CheckFeedback(Date(), checkResult)
        TaskToolWindowView.getInstance(project).apply {
          checkFinished(task, checkResult)
          updateCheckPanel(task)
        }
      }
    }
  }



}
