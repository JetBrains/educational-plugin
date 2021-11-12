package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.BrowserHyperlinkListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.checker.CheckResultSeverity
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
import java.awt.datatransfer.StringSelection
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

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
      var severity = CheckResultSeverity.Info
      var checkResult = CheckResult(checkStatus, message, severity = severity)
      var hyperlinkListener: HyperlinkListener? = null
      try {
        CodeforcesSettings.getInstance().account?.let {
          val responseMessage = CodeforcesConnector.getInstance().submitSolution(task, solution, it)
            .onError { errorMessage ->
              checkStatus = CheckStatus.Failed
              severity = CheckResultSeverity.Error
              hyperlinkListener = object : BrowserHyperlinkListener() {
                override fun hyperlinkActivated(e: HyperlinkEvent) {
                  CopyPasteManager.getInstance().setContents(StringSelection(solution))
                  super.hyperlinkActivated(e)
                }
              }
              errorMessage + "&emsp;<a href=${CodeforcesTask.codeforcesSubmitLink(task)}>${
                EduCoreBundle.message("codeforces.copy.solution.and.submit")
              }</a>"
            }
          if (responseMessage.isNotBlank()) {
            message = responseMessage
            if (severity == CheckResultSeverity.Info) severity = CheckResultSeverity.Warning
          }
          checkResult = CheckResult(checkStatus, message, severity = severity, hyperlinkListener = hyperlinkListener)
        }
      }
      catch (e: Exception) {
        LOG.error(e)
        checkResult = failedToCheck
      }
      finally {
        if (!severity.isWaring()) {
          task.feedback = CheckFeedback(checkResult.message)
          task.status = checkResult.status

          ProjectView.getInstance(project).refresh()
          YamlFormatSynchronizer.saveItem(task)
        }
        taskDescriptionView.checkFinished(task, checkResult)
      }
    }
  }

  companion object {

    private val LOG = Logger.getInstance(this::class.java)

    @NonNls
    const val ACTION_ID = "Educational.Codeforces.Submit.Solution"
  }

}