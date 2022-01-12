package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.stepik.PostSolutionCheckListener
import com.jetbrains.edu.learning.submissions.Submission

class MarketplaceCheckListener: PostSolutionCheckListener() {

  override fun EduCourse.isToPostSubmissions(): Boolean {
    if (!isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS)) return false
    val account = MarketplaceSettings.INSTANCE.account ?: return false
    return isMarketplaceRemote && account.isJwtTokenProvided()
  }

  override fun postSubmission(project: Project, task: Task): Submission {
    return MarketplaceSolutionLoader.getInstance(project).postSubmission(project, task)
  }

  override fun isUpToDate(course: EduCourse, task: Task): Boolean = course.isUpToDate

  override fun updateCourseAction(project: Project, course: EduCourse) = course.checkForUpdates(project, true) {}
}