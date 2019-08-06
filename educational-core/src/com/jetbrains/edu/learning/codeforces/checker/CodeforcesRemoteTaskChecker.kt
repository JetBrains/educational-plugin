package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.EduEditor
import java.awt.datatransfer.StringSelection

class CodeforcesRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean = task is CodeforcesTask

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    val studyEditor = FileEditorManager.getInstance(project).selectedEditor
    val solution = (studyEditor as EduEditor).editor.document.text
    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    (task.course as CodeforcesCourse).submissionUrl.let {
      BrowserUtil.browse(it)
    }

    return CheckResult(CheckStatus.Unchecked, "")
  }
}