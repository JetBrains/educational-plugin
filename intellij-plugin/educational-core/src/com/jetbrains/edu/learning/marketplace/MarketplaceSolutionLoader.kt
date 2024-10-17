package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.SolutionLoaderBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.Companion.toCheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceStateOnClose
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionBase
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.*

@Service(Service.Level.PROJECT)
class MarketplaceSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override fun loadSolutionsInBackground() {
    MarketplaceConnector.getInstance().isLoggedInAsync().thenApplyAsync { isLoggedIn ->
      if (isLoggedIn) {
        super.loadSolutionsInBackground()
      }
    }
  }

  override fun loadSubmissions(tasks: List<Task>): List<Submission> {
    val submissions = super.loadSubmissions(tasks)
    if (!SubmissionSettings.getInstance(project).stateOnClose) return submissions
    val courseStateOnClose = SubmissionsManager.getInstance(project).getCourseStateOnClose()
    if (courseStateOnClose.isEmpty()) return submissions

    // Don't use `associateBy` or other friends here because we need the first submission for each task, not the last one
    val submissionsMap = mutableMapOf<Int, Submission>()
    for (submission in submissions) {
      submissionsMap.putIfAbsent(submission.taskId, submission)
    }

    return tasks.mapNotNull {
      val submission = submissionsMap[it.id]
      val stateOnClose = courseStateOnClose[it.id] as? MarketplaceStateOnClose
      combineSubmissions(submission, stateOnClose)
    }
  }

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    val lastSubmission = submissions.firstOrNull { it.taskId == task.id }
    val formatVersion = lastSubmission?.formatVersion ?: return TaskSolutions.EMPTY

    if (!isVersionCompatible(formatVersion)) return TaskSolutions.INCOMPATIBLE

    if (lastSubmission !is MarketplaceSubmissionBase)
      error(
        "Marketplace submission to apply ${lastSubmission.id} for task ${task.name} is not an instance " +
        "of the ${MarketplaceSubmissionBase::class.simpleName} class"
      )

    val submissionsSettings = SubmissionSettings.getInstance(project)

    // Is added specially for courses launched via Remote Development solution to keep user code in editor on course updates
    // To be fixed by EDU-7466
    if (!submissionsSettings.applySubmissionsForce && lastSubmission.courseVersion != task.course.marketplaceCourseVersion) {
      LOG.info(
        "Marketplace submission ${lastSubmission.id} for task ${task.name} is not up to date. " +
        "Submission course version: ${lastSubmission.courseVersion}, course version: ${task.course.marketplaceCourseVersion}"
      )
      return TaskSolutions.EMPTY
    }

    val files =
      when (task) {
        is TheoryTask -> if (!submissionsSettings.stateOnClose) emptyMap() else lastSubmission.eduTaskFiles()
        is ChoiceTask -> emptyMap()

        is OutputTask,
        is EduTask -> lastSubmission.eduTaskFiles()
        else -> {
          LOG.warn("Solutions for task ${task.name} of type ${task::class.simpleName} not loaded")
          emptyMap()
        }
      }

    return if (files.isEmpty() && task !is TheoryTask && task !is ChoiceTask) TaskSolutions.EMPTY
    else TaskSolutions(lastSubmission.time, lastSubmission.status?.toCheckStatus() ?: CheckStatus.Unchecked, files)
  }

  private fun combineSubmissions(lastSubmission: Submission?, lastState: MarketplaceStateOnClose?): Submission? {
    if (!SubmissionSettings.getInstance(project).stateOnClose || lastState == null) return lastSubmission
    return when {
      lastSubmission == null || (lastState.time?.after(lastSubmission.time) == true) -> {
        // EDU-7112 Ideally, it should be replaced with `lastState.copy(status = lastSubmission?.status)`
        // once we make the corresponding data immutable data classes
        DelegateMarketplaceSubmission(lastState, lastSubmission?.status)
      }
      else -> lastSubmission
    }
  }

  private fun MarketplaceSubmissionBase.eduTaskFiles(): Map<String, Solution> {
    if (solutionFiles == null) {
      solutionFiles = MarketplaceSubmissionsConnector.getInstance().loadSolutionFiles(solutionKey)
    }
    return solutionFiles?.associate { it.name to Solution(it.text, it.isVisible, it.placeholders ?: emptyList()) } ?: emptyMap()
  }

  companion object {
    fun getInstance(project: Project): MarketplaceSolutionLoader = project.service()

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }

  private class DelegateMarketplaceSubmission(private val delegate: MarketplaceSubmissionBase, status: String?) : MarketplaceSubmissionBase() {

    init {
      this.id = delegate.id
      this.time = delegate.time
      this.status = status ?: delegate.status
      this.courseVersion = delegate.courseVersion
      this.solutionKey = delegate.solutionKey
    }

    override var taskId: Int by delegate::taskId
    override var solutionFiles: List<SolutionFile>? by delegate::solutionFiles
    override var formatVersion: Int by delegate::formatVersion
  }
}