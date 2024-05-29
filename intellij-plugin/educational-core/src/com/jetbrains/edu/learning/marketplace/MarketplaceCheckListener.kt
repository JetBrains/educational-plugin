package com.jetbrains.edu.learning.marketplace

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSubmissionNotPostedNotification
import com.jetbrains.edu.learning.marketplace.actions.PostMarketplaceProjectToGitHub
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanners
import java.util.concurrent.CompletableFuture

class MarketplaceCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course as? EduCourse ?: return

    if (!course.isStudy || !course.isMarketplaceRemote || !task.isToSubmitToRemote) return

    val submissionsManager = SubmissionsManager.getInstance(project)
    MarketplaceConnector.getInstance().isLoggedInAsync().thenApplyAsync { isLoggedIn ->
      if (!isLoggedIn) return@thenApplyAsync

      if (course.isUpToDate) {
        val submission = MarketplaceSubmissionsConnector.getInstance().postSubmission(project, task, result)
        submissionsManager.addToSubmissionsWithStatus(task.id, task.status, submission)
      }
      else {
        showSubmissionNotPostedNotification(project, course, task.name)
      }
    }

    EduCounterUsageCollector.submissionSuccess(result.isSolved)

    if (!result.isSolved) return

    PostMarketplaceProjectToGitHub.promptIfNeeded(project)

    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      SubmissionsManager.getInstance(project).loadCommunitySubmissions(task)
    }, ProcessIOExecutorService.INSTANCE)

    if (SolutionSharingPromptCounter.shouldPrompt()) {
      SolutionSharingInlineBanners.promptToEnableSolutionSharing(project)
    }
  }
}