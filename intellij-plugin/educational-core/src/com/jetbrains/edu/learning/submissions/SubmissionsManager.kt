package com.jetbrains.edu.learning.submissions

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.LightTestAware
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createTopic
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.submissions.SubmissionsProvider
 */
@Service(Service.Level.PROJECT)
class SubmissionsManager(private val project: Project) : LightTestAware {

  private val submissions = ConcurrentHashMap<Int, List<Submission>>()

  private val communitySubmissions = ConcurrentHashMap<Int, CommunitySubmissions>()

  data class CommunitySubmissions(val submissions: MutableList<Submission>, var hasMore: Boolean = false)

  var course: Course? = project.course
    @TestOnly set

  fun getSubmissionsFromMemory(taskIds: Set<Int>): List<Submission> {
    return taskIds.mapNotNull { submissions[it] }.flatten().sortedByDescending { it.time }
  }

  fun getCommunitySubmissionsFromMemory(taskId: Int): List<Submission>? {
    return communitySubmissions[taskId]?.submissions?.sortedByDescending { it.time }
  }

  private fun getCommunitySubmissionFromMemory(taskId: Int, submissionId: Int): Submission? {
    return communitySubmissions[taskId]?.submissions?.find { it.id == submissionId }
  }

  fun getOrLoadSubmissions(tasks: List<Task>): List<Submission>? {
    val taskIds = tasks.map { it.id }.toSet()
    val submissionsFromMemory = getSubmissionsFromMemory(taskIds)
    return submissionsFromMemory.ifEmpty {
      val course = this.course ?: return null
      val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return null
      val submissionsById = submissionsProvider.loadSubmissions(tasks, course.id)
      submissions.putAll(submissionsById)
      notifySubmissionsChanged()
      submissionsById.values.flatten()
    }
  }

  fun getSubmissions(task: Task): List<Submission> {
    return getOrLoadSubmissions(task)
  }

  fun getSubmissionWithSolutionText(task: Task, submissionId: Int): Submission? {
    val submission = getOrLoadSubmissions(task).find { it.id == submissionId }
                     ?: getCommunitySubmissionFromMemory(task.id, submissionId)
                     ?: return null

    if (submission is MarketplaceSubmission && submission.solutionFiles == null) {
      val course = this.course ?: return null
      val submissionsProvider = course.getSubmissionsProvider() ?: return null
      submissionsProvider.loadSolutionFiles(submission)
    }
    return submission
  }

  private fun getOrLoadSubmissions(task: Task): List<Submission> {
    val course = this.course ?: return emptyList()
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[task.id]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadSubmissions(listOf(task), course.id)
      submissions.putAll(loadedSubmissions)
      notifySubmissionsChanged()
      return loadedSubmissions[task.id] ?: emptyList()
    }
  }

  fun addToSubmissions(taskId: Int, submission: Submission) {
    val submissionsList = submissions.getOrPut(taskId) { listOf(submission) }.toMutableList()
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      submissions[taskId] = submissionsList
      //potential race when loading submissions and checking task at one time
    }
    notifySubmissionsChanged()
  }

  private fun notifySubmissionsChanged() {
    project.messageBus.syncPublisher(TOPIC).submissionsChanged()
  }

  fun containsCorrectSubmission(stepId: Int): Boolean {
    return getSubmissionsFromMemory(setOf(stepId)).any { it.status == CORRECT }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = checkStatus.rawStatus
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = this.course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContentWhenLoggedIn(loadSolutions: () -> Unit = {}) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    CompletableFuture.runAsync({
      if (!isLoggedIn()) return@runAsync
      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      val platformName = getPlatformName()

      taskToolWindowView.showLoadingSubmissionsPanel(platformName)
      taskToolWindowView.showLoadingCommunityPanel(platformName)
      loadSubmissionsContent(course, submissionsProvider, loadSolutions)
      loadCommunityContent(course, submissionsProvider)

      notifySubmissionsChanged()
    }, ProcessIOExecutorService.INSTANCE)
  }

  fun removeCommunitySubmission(taskId: Int, submissionId: Int) {
    communitySubmissions[taskId]?.submissions?.removeIf { it.id == submissionId }
    notifySubmissionsChanged()
  }

  fun loadCommunitySubmissions(task: Task) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    submissionsProvider.isLoggedInAsync().thenApply { isLoggedIn ->
      if (!isLoggedIn) return@thenApply

      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      taskToolWindowView.showLoadingCommunityPanel(getPlatformName())
      val taskId = task.id
      val result = submissionsProvider.loadSharedSubmissions(course, task)
      if (result == null) {
        communitySubmissions[taskId]?.hasMore = false
        return@thenApply taskToolWindowView.updateSubmissionsTab()
      }
      val (sharedSubmissions, hasMore) = result
      communitySubmissions[taskId] = CommunitySubmissions(sharedSubmissions.toMutableList(), hasMore)
      notifySubmissionsChanged()
    }
  }

  fun loadMoreCommunitySubmissions(task: Task, latest: Int, oldest: Int) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    submissionsProvider.isLoggedInAsync().thenApply { isLoggedIn ->
      if (!isLoggedIn) return@thenApply

      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      taskToolWindowView.showLoadingCommunityPanel(getPlatformName())
      val taskId = task.id
      val result = submissionsProvider.loadMoreSharedSubmissions(course, task, latest, oldest)
      if (result == null) {
        communitySubmissions[taskId]?.hasMore = false
        return@thenApply taskToolWindowView.updateSubmissionsTab()
      }
      val (sharedSubmissions, hasMore) = result
      communitySubmissions[taskId]?.let {
        it.submissions.addAll(sharedSubmissions)
        it.hasMore = hasMore
      } ?: run {
        communitySubmissions[taskId] = CommunitySubmissions(sharedSubmissions.toMutableList(), hasMore)
      }
      notifySubmissionsChanged()
    }
  }

  private fun SubmissionsProvider.isLoggedInAsync(): CompletableFuture<Boolean> = CompletableFuture.supplyAsync({
    isLoggedIn()
  }, ProcessIOExecutorService.INSTANCE)

  private fun TaskToolWindowView.updateSubmissionsTab() {
    project.invokeLater {
      updateTab(TabType.SUBMISSIONS_TAB)
    }
  }

  fun deleteCourseSubmissionsLocally() {
    course?.allTasks?.forEach { submissions.remove(it.id) }
    notifySubmissionsChanged()
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  @RequiresBackgroundThread
  fun isSubmissionDownloadAllowed(): Boolean = course?.getSubmissionsProvider()?.isSubmissionDownloadAllowed() ?: false

  @RequiresBackgroundThread
  fun isSolutionSharingAllowed(): Boolean = course?.getSubmissionsProvider()?.isSolutionSharingAllowed() ?: false

  fun isCommunitySolutionsLoaded(task: Task): Boolean = !communitySubmissions[task.id]?.submissions.isNullOrEmpty()

  fun isAllowedToLoadCommunitySolutions(task: Task): Boolean {
    val submissions = submissions[task.id] ?: return false
    val correctSubmissions = submissions.count { it.status == CORRECT }
    val wrongSubmissions = submissions.count() - correctSubmissions
    return correctSubmissions >= 1 || gotStuckCondition(task, wrongSubmissions)
  }

  private fun gotStuckCondition(task: Task, numberOfWrongSubmissions: Int): Boolean {
    return !task.canShowSolution() && numberOfWrongSubmissions >= GOT_STUCK_WRONG_SUBMISSIONS_AMOUNT
  }

  private fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize(Runnable { prepareSubmissionsContentWhenLoggedIn() })

  private fun loadSubmissionsContent(course: Course, submissionsProvider: SubmissionsProvider, loadSolutions: () -> Unit) {
    submissions.putAll(submissionsProvider.loadAllSubmissions(course))
    loadSolutions()
  }

  private fun loadCommunityContent(course: Course, submissionsProvider: SubmissionsProvider) {
    val courseSharedSolutions = submissionsProvider.loadSharedSolutionsForCourse(course)
    courseSharedSolutions.forEach { (taskId, sharedSolutions) ->
      communitySubmissions[taskId] = CommunitySubmissions(sharedSolutions.toMutableList(), hasMore = true)
    }
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  fun hasMoreCommunitySubmissions(taskId: Int): Boolean {
    return communitySubmissions[taskId]?.hasMore ?: false
  }

  @TestOnly
  override fun cleanUpState() {
    submissions.clear()
  }

  @TestOnly
  fun addCommunitySolutions(taskId: Int, solutions: MutableList<Submission>) {
    communitySubmissions.putAll(mapOf(taskId to CommunitySubmissions(solutions)))
  }

  companion object {

    @Topic.ProjectLevel
    val TOPIC: Topic<SubmissionsListener> = createTopic("Edu.submissions")

    fun getInstance(project: Project): SubmissionsManager {
      return project.service()
    }

    private const val GOT_STUCK_WRONG_SUBMISSIONS_AMOUNT: Int = 3
  }
}

fun interface SubmissionsListener {
  fun submissionsChanged()
}