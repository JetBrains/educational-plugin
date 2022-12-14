package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import com.jetbrains.edu.learning.submissions.SubmissionsProvider

/**
 * Submissions for Marketplace are stored on Grazie Cloud Platform (`https://jetbrains.team/p/grazi/documents/Grazie-Platform/a/Overview-9kGDu0ELNuV`)
 * When the first submissions for Task is posted - meta-entity Document is created on Grazie side. Document has versions, which are
 * submissions in our terminology. So when the following submissions are being posted, one more Version is being added to the corresponding
 * Document.
 * @see  com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.createSubmissionsDocument
 * @see  com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.updateSubmissionsDocument
 * Task is connected with the document by a path: courseId/taskId/submissionId posted with WorkspaceAPI (`https://jetbrains.team/p/grazi/documents/Grazie-Platform/a/Workspace-API`)
 * @see com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.addDocumentPath
 *
 * To get submissions for the course from Grazie the following steps should be taken:
 * 1. get the descriptors list for the course, containing pairs documentId - path (which contains taskId)
 * @see com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.getDescriptorsList
 * 2. get VersionIds for the corresponding documentId
 * @see com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.getDocVersionsIds
 * 3. load submission for the corresponding documentId and versionId
 * @see com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector.getSubmission
 */

class MarketplaceSubmissionsProvider : SubmissionsProvider {
  override fun loadAllSubmissions(project: Project, course: Course): Map<Int, List<MarketplaceSubmission>> {
    if (course is EduCourse && course.isMarketplaceRemote && isLoggedIn()) {
      return MarketplaceSubmissionsConnector.getInstance().getAllSubmissions(course)
    }
    return emptyMap()
  }

  override fun loadSubmissions(tasks: List<Task>, courseId: Int): Map<Int, List<MarketplaceSubmission>> {
    return tasks.associate { Pair(it.id, MarketplaceSubmissionsConnector.getInstance().getSubmissions(it, courseId)) }
  }

  override fun areSubmissionsAvailable(course: Course): Boolean {
    return isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS) && course is EduCourse && course.isStudy && course.isMarketplaceRemote
  }

  override fun isLoggedIn(): Boolean {
    return MarketplaceConnector.getInstance().isLoggedIn()
  }

  override fun getPlatformName(): String = MARKETPLACE

  override fun doAuthorize() {
    MarketplaceConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.SUBMISSIONS_TAB)
  }
}