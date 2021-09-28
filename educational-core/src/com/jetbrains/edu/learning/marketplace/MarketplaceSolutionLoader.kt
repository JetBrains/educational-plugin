package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.SubmissionDocument
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionData
import com.jetbrains.edu.learning.submissions.getSolutionFiles

class MarketplaceSolutionLoader(project: Project) : SolutionLoaderBase(project) {
  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    TODO("Not yet implemented")
  }

  override fun provideTasksToUpdate(course: Course): List<Task> {
    TODO("Not yet implemented")
  }

  fun postSubmission(project: Project, task: Task): Submission? {
    val submissionData = createSubmissionData(task) ?: return null
    val submissionDocument = SubmissionDocument(docId = task.submissionsId,
                                                submissionContent = ObjectMapper().writeValueAsString(submissionData).trimIndent())
    val connector = MarketplaceSubmissionsConnector.getInstance()
    if (task.submissionsId == null) {
      val submissionId = submissionData.submission.id ?: error("Submission id not generated at creation")
      connector.createSubmissionsDocument(project, submissionDocument, task, submissionId)
    }
    else {
      connector.updateSubmissionsDocument(project, submissionDocument, task)
    }
    return submissionData.submission
  }

  private fun createSubmissionData(task: Task): SubmissionData? {
    val passed = task.status == CheckStatus.Solved
    val solutionFiles = getSolutionFiles(project, task, LOG)?.filter { it.isVisible } ?: return null
    return SubmissionData(passed, solutionFiles, task)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): MarketplaceSolutionLoader = ServiceManager.getService(project, MarketplaceSolutionLoader::class.java)

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }
}