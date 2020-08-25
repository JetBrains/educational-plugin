package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.editor.EduEditor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.datatransfer.StringSelection

@Suppress("ComponentNotRegistered") // Codeforces.xml
class CodeforcesCopyAndSubmitAction : DumbAwareAction(EduCoreBundle.message("codeforces.copy.and.submit")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = EduState.getEduState(project)?.task as? CodeforcesTask ?: return
    val course = task.course as? CodeforcesCourse ?: return

    val studyEditor = FileEditorManager.getInstance(project).selectedEditor
    val solution = (studyEditor as EduEditor).editor.document.text
    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    BrowserUtil.browse(course.getSubmissionUrl())
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return

    val task = EduState.getEduState(project)?.task ?: return
    if (task !is CodeforcesTask) return

    presentation.isEnabledAndVisible = true
  }

  companion object {
    const val ACTION_ID = "Codeforces.CopyAndSubmit"
  }
}
