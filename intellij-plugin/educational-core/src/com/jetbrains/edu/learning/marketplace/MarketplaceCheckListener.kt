package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.PostSolutionCheckListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
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

    CompletableFuture.supplyAsync {
      SubmissionsManager.getInstance(project).isSolutionSharingAllowed()
    }.thenApply { isSolutionSharingAllowed ->
      if (isSolutionSharingAllowed) {
        project.invokeLater {
          if (SolutionSharingPromptCounter.shouldPrompt() && project.isStudentProject()) {
            SolutionSharingInlineBanners.promptToEnableSolutionSharing(project)
          }
        }
      }
    }
  }
}