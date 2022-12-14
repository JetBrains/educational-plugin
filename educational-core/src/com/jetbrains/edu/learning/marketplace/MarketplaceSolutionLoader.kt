package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.SolutionLoaderBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.Companion.toCheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.isVersionCompatible

class MarketplaceSolutionLoader(project: Project) : SolutionLoaderBase(project) {
  override fun loadSolutionsInBackground() {
    val account = MarketplaceSettings.INSTANCE.hubAccount ?: return
    if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS) && account.isJwtTokenProvided()) {
      super.loadSolutionsInBackground()
    }
  }

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    // make sure the first submission is the latest after switching to a separate service on grazie
    val lastSubmission = submissions.firstOrNull { it.taskId == task.id }
    val formatVersion = lastSubmission?.formatVersion ?: return TaskSolutions.EMPTY

    if (!isVersionCompatible(formatVersion)) return TaskSolutions.INCOMPATIBLE

    if (lastSubmission !is MarketplaceSubmission)
      error("Marketplace submission ${lastSubmission.id} for task ${task.name} is not instance " +
            "of ${MarketplaceSubmission::class.simpleName} class")

    if (lastSubmission.courseVersion != task.course.marketplaceCourseVersion) {
      LOG.info("Marketplace submission ${lastSubmission.id} for task ${task.name} is not up to date. " +
               "Submission course version: ${lastSubmission.courseVersion}, course version: ${task.course.marketplaceCourseVersion}")
      return TaskSolutions.INCOMPATIBLE
    }

    val files =
      when (task) {
        is TheoryTask,
        is ChoiceTask -> emptyMap()
        is EduTask -> lastSubmission.eduTaskFiles()
        else -> {
          LOG.warn("Solutions for task ${task.name} of type ${task::class.simpleName} not loaded")
          emptyMap()
        }
      }

    return if (files.isEmpty() && task !is TheoryTask && task !is ChoiceTask) TaskSolutions.EMPTY
    else TaskSolutions(lastSubmission.time, lastSubmission.status?.toCheckStatus() ?: CheckStatus.Unchecked, files)
  }

  private fun MarketplaceSubmission.eduTaskFiles(): Map<String, Solution> {
    return solutionFiles?.associate { it.name to Solution(it.text, it.isVisible, it.placeholders ?: emptyList()) } ?: emptyMap()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): MarketplaceSolutionLoader = project.service()

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }
}