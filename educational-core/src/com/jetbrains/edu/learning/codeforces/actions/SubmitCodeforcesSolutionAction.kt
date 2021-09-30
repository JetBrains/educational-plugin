package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import org.jetbrains.annotations.NonNls

class SubmitCodeforcesSolutionAction : CodeforcesAction(EduCoreBundle.lazyMessage("action.codeforces.submit.solution")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = EduUtils.getCurrentTask(project) as? CodeforcesTask ?: return
    val solution = task.getCodeTaskFile(project)?.getDocument(project)?.text ?: return

    CodeforcesSettings.getInstance().account?.let {
      CodeforcesConnector.getInstance().submitSolution(task, solution, it).onError {
        ApplicationManager.getApplication().messageBus.syncPublisher(ERROR_TOPIC).error(it)
      }
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Codeforces.SubmitSolution"
    val ERROR_TOPIC = Topic.create("Codeforces.Submission.Error", CodeforcesSubmissionErrorListener::class.java)
  }

}