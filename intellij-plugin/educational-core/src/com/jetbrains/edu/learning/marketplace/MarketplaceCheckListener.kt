package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.stepik.PostSolutionCheckListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanners

class MarketplaceCheckListener: PostSolutionCheckListener() {

  override fun EduCourse.isToPostSubmissions(): Boolean = isMarketplaceRemote

  override fun postSubmission(project: Project, task: Task, result: CheckResult): MarketplaceSubmission {
    return MarketplaceSubmissionsConnector.getInstance().postSubmission(project, task, result)
  }

  override fun isUpToDate(course: EduCourse, task: Task): Boolean = course.isUpToDate

  override fun updateCourseAction(project: Project, course: EduCourse) = course.checkForUpdates(project, true) {}

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    super.afterCheck(project, task, result)

    if (!Registry.`is`(ShareMySolutionsAction.REGISTRY_KEY, false)) return

    if (!result.isSolved || !task.supportSubmissions || !project.isStudentProject()) return

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (submissionsManager.isFirstCorrectSubmissionForTask(task)) {
      submissionsManager.loadCommunitySubmissions(task)
    }

    if (SolutionSharingPromptCounter.shouldPrompt()) {
      SolutionSharingInlineBanners.promptToEnableSolutionSharing(project)
    }
  }

  private fun SubmissionsManager.isFirstCorrectSubmissionForTask(task: Task): Boolean {
    val correctSubmissions = getSubmissions(task).count { it.status == EduFormatNames.CORRECT }
    return correctSubmissions == 1
  }
}