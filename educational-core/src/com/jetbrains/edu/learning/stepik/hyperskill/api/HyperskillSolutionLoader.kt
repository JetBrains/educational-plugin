package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.mockTaskFileName
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.openSelectedStage
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager

class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override val loadingTopic: Topic<SolutionLoadingListener> = SOLUTION_TOPIC

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    val lastSubmission = submissions.firstOrNull { it.step == task.id }
    val reply = lastSubmission?.reply ?: return TaskSolutions.EMPTY

    val files: Map<String, Solution> = when (task) {
      is EduTask -> reply.eduTaskFiles
      is CodeTask -> reply.codeTaskFiles(task)
      else -> {
        LOG.warn("Solutions for task ${task.name} of type ${task::class.simpleName} not loaded")
        emptyMap()
      }
    }

    return if (files.isEmpty()) TaskSolutions.EMPTY
    else TaskSolutions(lastSubmission.time, lastSubmission.status.toCheckStatus(), files)
  }

  override fun loadSubmissions(tasks: List<Task>): List<Submission>? {
    return SubmissionsManager.getInstance(project).getSubmissions(tasks.map { it.id }.toSet())
  }

  private val Reply.eduTaskFiles: Map<String, Solution>
    get() = solution?.associate { it.name to Solution(it.text, it.isVisible, emptyList()) } ?: emptyMap()

  private fun Reply.codeTaskFiles(task: CodeTask): Map<String, Solution> {
    val codeFromServer = code ?: return emptyMap()
    val taskFileName = task.mockTaskFileName ?: return emptyMap()
    return mapOf(taskFileName to Solution(codeFromServer, true, emptyList()))
  }

  override fun provideTasksToUpdate(course: Course): List<Task> {
    return course.items.asSequence().flatMap {
      when (it) {
        is Lesson -> sequenceOf(it)
        is Section -> it.items.asSequence().filterIsInstance<Lesson>()
        else -> emptySequence()
      }
    }.flatMap { it.taskList.asSequence() }.toList()
  }

  override fun updateTasks(course: Course,
                           tasks: List<Task>,
                           submissions: List<Submission>,
                           progressIndicator: ProgressIndicator?,
                           force: Boolean) {
    super.updateTasks(course, tasks, submissions, progressIndicator, force)
    runInEdt {
      openSelectedStage(course, project)
    }
  }

  private fun String?.toCheckStatus(): CheckStatus = when (this) {
    EduNames.WRONG -> CheckStatus.Failed
    EduNames.CORRECT -> CheckStatus.Solved
    else -> CheckStatus.Unchecked
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillSolutionLoader = ServiceManager.getService(project, HyperskillSolutionLoader::class.java)

    val SOLUTION_TOPIC: Topic<SolutionLoadingListener> = Topic.create("Hyperskill solutions loaded", SolutionLoadingListener::class.java)
    private val LOG = Logger.getInstance(HyperskillSolutionLoader::class.java)
  }
}

