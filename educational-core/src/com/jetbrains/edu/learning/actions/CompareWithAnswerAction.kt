package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtils.putSelectedTaskFileFirst
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

open class CompareWithAnswerAction : DumbAwareAction("Compare with Answer", "Compare your solution with answer", AllIcons.Actions.Diff) {
  companion object {
    const val ACTION_ID = "Educational.CompareWithAnswer"
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val studyEditor = EduUtils.getSelectedEduEditor(project)
    val studyState = EduState(studyEditor)
    if (!studyState.isValid) {
      return
    }

    val task = studyState.task

    val taskFiles = getTaskFiles(task)
    putSelectedTaskFileFirst(taskFiles, studyState.taskFile!!)

    val requests = taskFiles.mapNotNull {
      val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
      val studentFileContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
      val solution = getSolution(it) ?: return@mapNotNull null
      val solutionFileContent = DiffContentFactory.getInstance().create(solution, virtualFile.fileType)
      SimpleDiffRequest("Compare your solution with answer", studentFileContent, solutionFileContent, virtualFile.name,
                        "${virtualFile.name} Answer")
    }
    if (requests.isEmpty()) {
      val message = JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder("No solution provided for this step", MessageType.INFO, null)
      message.createBalloon().show(JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext), Balloon.Position.above)
      return
    }
    DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
    EduCounterUsageCollector.solutionPeeked()
  }

  protected open fun getTaskFiles(task: Task) =
    task.taskFiles.values.filter { it.answerPlaceholders.isNotEmpty() }.toMutableList()

  protected open fun getSolution(taskFile: TaskFile): String? {
    val fullAnswer = StringBuilder(taskFile.text)

    taskFile.answerPlaceholders?.sortedBy { it.offset }?.reversed()?.forEach { placeholder ->
      placeholder.possibleAnswer?.let { answer ->
        fullAnswer.replace(placeholder.initialState.offset,
                           placeholder.initialState.offset + placeholder.initialState.length, answer)
      }
    }
    return fullAnswer.toString()
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) {
      return
    }
    val task = EduUtils.getCurrentTask(project) ?: return
    presentation.isEnabledAndVisible = canShowSolution(task)
  }

  protected open fun canShowSolution(task: Task) = task.canShowSolution()
}