package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution

class CompareWithAnswerAction : DumbAwareAction("Compare with Answer", "Compare your solution with answer", AllIcons.Diff.Diff) {
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

    val virtualFile = studyState.virtualFile ?: return
    val document = studyEditor?.editor?.document ?: return

    val taskFileContent = DiffContentFactory.getInstance().create(project, document.immutableCharSequence.toString())

    val answerText = getFileTextWithAnswers(project, studyEditor.taskFile.getText())
    val answerFileName = "answer." + virtualFile.extension
    val answerContent = DiffContentFactory.getInstance().create(answerText, virtualFile.fileType)

    val request = SimpleDiffRequest(
      "Compare your solution with answer",
      taskFileContent, answerContent,
      virtualFile.name, answerFileName)

    DiffManager.getInstance().showDiff(project, request)
  }

  private fun getFileTextWithAnswers(project: Project, text: String): String {
    val fullAnswer = StringBuilder(text)

    val studyState = EduState(EduUtils.getSelectedEduEditor(project))
    val taskFile = studyState.taskFile
    taskFile?.answerPlaceholders?.sortedBy { it.offset }?.reversed()?.forEach { placeholder ->
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