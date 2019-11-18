package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.editor.EduEditor
import java.awt.datatransfer.StringSelection

class CodeforcesTaskWithFileIOTaskChecker(task: CodeforcesTaskWithFileIO, project: Project) :
  TaskChecker<CodeforcesTaskWithFileIO>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val studyEditor = FileEditorManager.getInstance(project).selectedEditor
    val solution = (studyEditor as EduEditor).editor.document.text
    val url = (task.course as CodeforcesCourse).submissionUrl

    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    BrowserUtil.browse(url)
    return CheckResult(CheckStatus.Unchecked, "")
  }
}