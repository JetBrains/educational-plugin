package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask.Companion.codeforcesSubmitLink
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.selectedEditor
import java.awt.datatransfer.StringSelection

class CodeforcesTaskWithFileIOTaskChecker(task: CodeforcesTaskWithFileIO, project: Project) :
  TaskChecker<CodeforcesTaskWithFileIO>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val selectedEditor = project.selectedEditor ?: error("no selected editor for Codeforces check")
    val solution = selectedEditor.document.text
    val url = codeforcesSubmitLink(task)

    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    EduBrowser.getInstance().browse(url)
    return CheckResult(CheckStatus.Unchecked, "")
  }
}