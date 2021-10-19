package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtils.putSelectedTaskFileFirst
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_SOLUTIONS_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import org.jetbrains.annotations.NonNls

open class CompareWithAnswerAction : DumbAwareAction() {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.CompareWithAnswer"
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val eduState = project.eduState ?: return

    val (_, _, taskFile, task) = eduState

    if (task.course is HyperskillCourse) {
      val url = hyperskillTaskLink(task)
      EduBrowser.getInstance().browse("$url$HYPERSKILL_SOLUTIONS_ANCHOR")
      return
    }

    val taskFiles = getTaskFiles(task)
    putSelectedTaskFileFirst(taskFiles, taskFile)

    val requests = taskFiles.mapNotNull {
      val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
      val studentFileContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
      val solution = getSolution(it)
      val solutionFileContent = DiffContentFactory.getInstance().create(solution, virtualFile.fileType)
      SimpleDiffRequest(EduCoreBundle.message("action.Educational.CompareWithAnswer.description"), studentFileContent, solutionFileContent, virtualFile.name,
                        "${virtualFile.name} Answer")
    }
    if (requests.isEmpty()) {
      val message = JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder(EduCoreBundle.message("action.Educational.CompareWithAnswer.popup.content.no.solution.provided"), MessageType.INFO, null)
      message.createBalloon().show(JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext), Balloon.Position.above)
      return
    }
    showSolution(project, requests)
    EduCounterUsageCollector.solutionPeeked()
  }

  protected open fun showSolution(project: Project, requests: List<SimpleDiffRequest>) {
    DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
  }

  private fun getTaskFiles(task: Task) =
    task.taskFiles.values.filter { it.answerPlaceholders.isNotEmpty() }.toMutableList()

  private fun getSolution(taskFile: TaskFile): String {
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

    presentation.isEnabledAndVisible = task.canShowSolution()
  }
}