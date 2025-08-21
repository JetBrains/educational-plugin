package com.jetbrains.edu.learning.marketplace.submissions

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.submissions.TaskCommunitySubmissions
import com.jetbrains.edu.learning.submissions.provider.CommunitySubmissionsProvider
import com.jetbrains.edu.learning.submissions.provider.SubmissionsData

class MarketplaceCommunitySubmissionsProvider : CommunitySubmissionsProvider {
  override val platformName: String = JET_BRAINS_ACCOUNT

  private val marketplaceSubmissionsConnector: MarketplaceSubmissionsConnector
    get() = MarketplaceSubmissionsConnector.getInstance()

  override fun loadCommunitySubmissions(course: Course): SubmissionsData {
    return marketplaceSubmissionsConnector.getSharedSolutionsForCourse(course.id, course.marketplaceCourseVersion).groupBy { it.taskId }
  }

  override fun loadCommunitySubmissions(course: Course, task: Task): TaskCommunitySubmissions? {
    return marketplaceSubmissionsConnector.getSharedSubmissionsForTask(course, task.id)
  }

  override fun loadMoreCommunitySubmissions(course: Course, task: Task, latest: Int, oldest: Int): TaskCommunitySubmissions? {
    return marketplaceSubmissionsConnector.getMoreSharedSubmissions(course, task.id, latest, oldest)
  }

  override fun isAvailable(course: Course): Boolean {
    return course is EduCourse && course.isMarketplaceRemote && course.isStudy && !course.isFromCourseStorage()
  }
}
