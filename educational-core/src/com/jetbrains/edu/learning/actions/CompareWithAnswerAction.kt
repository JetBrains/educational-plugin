package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyState
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.StudyUtils

class CompareWithAnswerAction : DumbAwareAction("Compare with Answer", "Compare your solution with answer", AllIcons.Diff.Diff) {
    companion object {
        @JvmField val ACTION_ID = "Educational.CompareWithAnswer"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val studyState = StudyState(StudyUtils.getSelectedStudyEditor(project))
        if (!studyState.isValid) {
            return
        }

        val myVirtualFile = studyState.virtualFile ?: return
        val myFileContent = DiffContentFactory.getInstance().create(project, myVirtualFile)

        val myDocument = StudyUtils.getSelectedEditor(project)?.document ?: return
        val answerText = getFileTextWithAnswers(project, myDocument)
        val answerFileName = "answer." + myVirtualFile.extension
        val answerContent = DiffContentFactory.getInstance().create(answerText, myVirtualFile.fileType)

        val request = SimpleDiffRequest(
                "Compare your solution with answer",
                myFileContent, answerContent,
                myVirtualFile.name, answerFileName)

        DiffManager.getInstance().showDiff(project, request)
    }

    private fun getFileTextWithAnswers(project: Project, myDocument: Document): String {
        val fullAnswer = StringBuilder(myDocument.text)

        val studyState = StudyState(StudyUtils.getSelectedStudyEditor(project))
        studyState.taskFile.activePlaceholders
                .sortedBy { it.offset }
                .reversed()
                .forEach { placeholder ->
                    placeholder.possibleAnswer?.let { answer ->
                        fullAnswer.replace(placeholder.offset, placeholder.offset + placeholder.realLength, answer)
                    }
                }

        return fullAnswer.toString()
    }

    override fun update(e: AnActionEvent?) {
        StudyUtils.updateAction(e!!)
        val project = e.project
        if (project != null) {
            val course = StudyTaskManager.getInstance(project).course
            val presentation = e.presentation
            if (course != null && !course.isStudy) {
                presentation.isEnabled = false
                presentation.isVisible = true
                return
            }
            val studyEditor = StudyUtils.getSelectedStudyEditor(project)
            val studyState = StudyState(studyEditor)
            if (!studyState.isValid) {
                presentation.isEnabledAndVisible = false
                return
            }
            val taskFile = studyState.taskFile
            if (taskFile.activePlaceholders.isEmpty()) {
                presentation.isEnabledAndVisible = false
            }
        }
    }
}