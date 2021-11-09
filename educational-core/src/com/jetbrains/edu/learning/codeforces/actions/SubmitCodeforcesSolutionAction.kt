package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
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

class SubmitCodeforcesSolutionAction : CodeforcesAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val task = EduUtils.getCurrentTask(project) as? CodeforcesTask ?: return
    val solution = task.getCodeTaskFile(project)?.getDocument(project)?.text ?: return

    val taskDescriptionView = TaskDescriptionView.getInstance(project)
    taskDescriptionView.checkStarted(task, true)

    ApplicationManager.getApplication().executeOnPooledThread {
      var checkStatus = CheckStatus.Unchecked
      var message = EduCoreBundle.message("codeforces.message.solution.submitted",
                                          CODEFORCES_CONTEST_SUBMISSIONS_URL.format(task.course.id))
      var isWarning = false
      var checkResult = CheckResult(checkStatus, message, isWarning = isWarning)
      try {
        CodeforcesSettings.getInstance().account?.let {
          val responseMessage = CodeforcesConnector.getInstance().submitSolution(task, solution, it).onError { errorMessage ->
            checkStatus = CheckStatus.Failed
            errorMessage
          }
          if (responseMessage.isNotBlank()) {
            message = responseMessage
            isWarning = true
          }
          checkResult = CheckResult(checkStatus, message, isWarning = isWarning)
        }
      }
      catch (e: Exception) {
        checkResult = failedToCheck
      }
      finally {
        task.feedback = CheckFeedback(checkResult.message)
        task.status = checkResult.status

        ProjectView.getInstance(project).refresh()
        YamlFormatSynchronizer.saveItem(task)
        taskDescriptionView.checkFinished(task, checkResult)
      }
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Codeforces.Submit.Solution"
  }

}