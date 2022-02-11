package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.SubmissionDocument
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.checkNotEmpty
import com.jetbrains.edu.learning.submissions.findTaskFileInDirWithSizeCheck

class MarketplaceSolutionLoader(project: Project) : SolutionLoaderBase(project) {
  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    TODO("Not yet implemented")
  }

  override fun provideTasksToUpdate(course: Course): List<Task> {
    TODO("Not yet implemented")
  }

  fun postSubmission(project: Project, task: Task): MarketplaceSubmission {
    val submission = MarketplaceSubmission(task.id, task.status, getSolutionFiles(project, task), task.course.marketplaceCourseVersion)
    val connector = MarketplaceSubmissionsConnector.getInstance()
    val submissionDocument = SubmissionDocument(docId = task.submissionsId,
                                                submissionContent = connector.objectMapper.writeValueAsString(submission).trimIndent())
    if (task.submissionsId == null) {
      val submissionId = submission.id ?: error("Submission id not generated at creation")
      connector.createSubmissionsDocument(project, submissionDocument, task, submissionId)
    }
    else {
      connector.updateSubmissionsDocument(project, submissionDocument, task)
    }
    return submission
  }

  private fun getSolutionFiles(project: Project, task: Task): List<SolutionFile> {
    val files = mutableListOf<SolutionFile>()
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

    for (taskFile in task.taskFiles.values) {
      val virtualFile = findTaskFileInDirWithSizeCheck(taskFile, taskDir) ?: continue

      ApplicationManager.getApplication().runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text, taskFile.isVisible, taskFile.answerPlaceholders))
      }
    }

    return files.checkNotEmpty()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): MarketplaceSolutionLoader = project.service()

    private val LOG = Logger.getInstance(MarketplaceSolutionLoader::class.java)
  }
}