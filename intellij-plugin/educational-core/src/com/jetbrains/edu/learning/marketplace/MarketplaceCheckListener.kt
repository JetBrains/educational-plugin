package com.jetbrains.edu.learning.marketplace

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.PostSolutionCheckListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.isSolutionSharingAllowed
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanners
import java.util.concurrent.CompletableFuture

class MarketplaceCheckListener: PostSolutionCheckListener() {

  override fun EduCourse.isToPostSubmissions(): Boolean = isMarketplaceRemote

  override fun postSubmission(project: Project, task: Task, result: CheckResult): MarketplaceSubmission {
    return MarketplaceSubmissionsConnector.getInstance().postSubmission(project, task, result)
  }

  override fun isUpToDate(course: EduCourse, task: Task): Boolean = course.isUpToDate

  override fun updateCourseAction(project: Project, course: EduCourse) = course.checkForUpdates(project, true) {}

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    super.afterCheck(project, task, result)
    EduCounterUsageCollector.submissionSuccess(result.isSolved)

    if (!result.isSolved || !task.supportSubmissions || !project.isStudentProject()) return

    CompletableFuture.supplyAsync({
      MarketplaceSubmissionsConnector.getInstance().getUserAgreementState().isSolutionSharingAllowed()
    }, ProcessIOExecutorService.INSTANCE).thenApply { isSolutionSharingAllowed ->
      if (!isSolutionSharingAllowed) return@thenApply

      val submissionsManager = SubmissionsManager.getInstance(project)
      if (submissionsManager.isFirstCorrectSubmissionForTask(task)) {
        submissionsManager.loadCommunitySubmissions(task)
      }

      project.invokeLater {
        if (SolutionSharingPromptCounter.shouldPrompt() && project.isStudentProject()) {
          SolutionSharingInlineBanners.promptToEnableSolutionSharing(project)
        }
      }
    }
  }

  private fun SubmissionsManager.isFirstCorrectSubmissionForTask(task: Task): Boolean {
    val correctSubmissions = getSubmissions(task).count { it.status == EduFormatNames.CORRECT }
    return correctSubmissions == 1
  }
}