package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.actions.CodeforcesAction
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask.Companion.codeforcesSubmitLink
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.datatransfer.StringSelection

@Suppress("ComponentNotRegistered") // Codeforces.xml
class CodeforcesCopyAndSubmitAction : CodeforcesAction(EduCoreBundle.lazyMessage("codeforces.copy.and.submit")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = EduUtils.getCurrentTask(project) as? CodeforcesTask ?: return
    val taskFile = task.getCodeTaskFile(project) ?: return
    val solution = taskFile.getDocument(project)?.text ?: return

    CopyPasteManager.getInstance().setContents(StringSelection(solution))
    EduBrowser.getInstance().browse(codeforcesSubmitLink(task))
  }

  companion object {
    const val ACTION_ID = "Codeforces.CopyAndSubmit"
  }
}
