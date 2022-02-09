package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.SubmissionDocument
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createMarketplaceSubmissionData
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.getSolutionFiles

class MarketplaceSolutionLoader(project: Project) : SolutionLoaderBase(project) {
  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    TODO("Not yet implemented")
  }

  override fun provideTasksToUpdate(course: Course): List<Task> {
    TODO("Not yet implemented")
  }

  fun postSubmission(project: Project, task: Task): StepikBasedSubmission {
    val solutionFiles = getSolutionFiles(project, task).filter { it.isVisible }
    val submissionData = createMarketplaceSubmissionData(task, solutionFiles)
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

  companion object {
    @JvmStatic
    fun getInstance(project: Project): MarketplaceSolutionLoader = project.service()

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }
}