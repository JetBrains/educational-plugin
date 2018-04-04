package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager

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
    val answerText = getFileTextWithAnswers(project, document)
    val answerFileName = "answer." + virtualFile.extension
    val answerContent = DiffContentFactory.getInstance().create(answerText, virtualFile.fileType)

    val request = SimpleDiffRequest(
      "Compare your solution with answer",
      taskFileContent, answerContent,
      virtualFile.name, answerFileName)

    DiffManager.getInstance().showDiff(project, request)
  }

  private fun getFileTextWithAnswers(project: Project, myDocument: Document): String {
    val fullAnswer = StringBuilder(myDocument.text)

    val studyState = EduState(EduUtils.getSelectedEduEditor(project))
    val taskFile = studyState.taskFile
    taskFile?.answerPlaceholders?.sortedBy { it.offset }?.reversed()?.forEach { placeholder ->
      placeholder.possibleAnswer?.let { answer ->
        fullAnswer.replace(placeholder.offset, placeholder.offset + placeholder.realLength, answer)
      }
    }

    return fullAnswer.toString()
  }

  override fun update(e: AnActionEvent?) {
    EduUtils.updateAction(e!!)
    val project = e.project
    if (project != null) {
      val course = StudyTaskManager.getInstance(project).course
      val presentation = e.presentation
      if (course != null && !course.isStudy) {
        presentation.isEnabled = false
        presentation.isVisible = true
        return
      }
      val studyEditor = EduUtils.getSelectedEduEditor(project)
      val studyState = EduState(studyEditor)
      if (!studyState.isValid) {
        presentation.isEnabledAndVisible = false
        return
      }
      val taskFile = studyState.taskFile
      if (taskFile == null || taskFile.answerPlaceholders.isEmpty()) {
        presentation.isEnabledAndVisible = false
      }
    }
  }
}