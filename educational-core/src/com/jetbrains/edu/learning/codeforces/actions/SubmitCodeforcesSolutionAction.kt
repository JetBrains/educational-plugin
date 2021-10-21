package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_CONTEST_SUBMISSIONS_URL
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import java.util.*

class SubmitCodeforcesSolutionAction : CodeforcesAction(EduCoreBundle.lazyMessage("action.codeforces.submit.solution")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = EduUtils.getCurrentTask(project) as? CodeforcesTask ?: return
    val solution = task.getCodeTaskFile(project)?.getDocument(project)?.text ?: return
    TaskDescriptionView.getInstance(project).checkStarted(task)
    CodeforcesSettings.getInstance().account?.let {
      var checkStatus = CheckStatus.RemoteSubmitted
      var message = EduCoreBundle.message("codeforces.message.solution.submitted",
                                          CODEFORCES_CONTEST_SUBMISSIONS_URL.format(task.course.id))
      CodeforcesConnector.getInstance().submitSolution(task, solution, it).onError { errorMessage ->
        checkStatus = CheckStatus.SubmissionFailed
        message = EduCoreBundle.message("codeforces.failed.to.submit.solution", errorMessage)
      }
      val checkResult = CheckResult(checkStatus, message)

      task.feedback = CheckFeedback(Date(), checkResult)
      task.status = checkStatus

      ProjectView.getInstance(project).refresh()
      YamlFormatSynchronizer.saveItem(task)
      TaskDescriptionView.getInstance(project).checkFinished(task, checkResult)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Codeforces.SubmitSolution"
  }

}