package com.jetbrains.edu.learning.submissions.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.submissions.TaskCommunitySubmissions

interface CommunitySubmissionsProvider {
  val platformName: String

  fun loadCommunitySubmissions(course: Course): SubmissionsData

  /**
   * Loads shared submissions for a specific [task].
   */
  fun loadCommunitySubmissions(course: Course, task: Task): TaskCommunitySubmissions?

  /**
   * Loads shared submissions that are not yet in the [com.jetbrains.edu.learning.submissions.SubmissionsManager] for a specific [task].
   *
   * @param latest id of the most recently loaded shared solution
   * @param oldest id of the least recently loaded shared solution
   */
  fun loadMoreCommunitySubmissions(course: Course, task: Task, latest: Int, oldest: Int): TaskCommunitySubmissions?

  fun isAvailable(course: Course): Boolean

  companion object {
    private val EP_NAME = ExtensionPointName.create<CommunitySubmissionsProvider>("Educational.communitySubmissionsProvider")

    fun Course.getCommunitySubmissionsProvider(): CommunitySubmissionsProvider? {
      val communitySubmissionsProviders = EP_NAME.extensionList.filter { it.isAvailable(this) }.ifEmpty { return null }
      return communitySubmissionsProviders.single()
    }
  }
}