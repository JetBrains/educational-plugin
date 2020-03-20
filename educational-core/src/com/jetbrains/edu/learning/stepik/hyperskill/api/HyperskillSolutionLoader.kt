package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.hyperskill.openSelectedStage

class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override val loadingTopic: Topic<SolutionLoadingListener> = SOLUTION_TOPIC

  override fun loadSolution(task: Task): TaskSolutions {
    val lastSubmission = HyperskillConnector.getInstance().getSubmission(task.id)
    val reply = lastSubmission?.reply ?: return TaskSolutions.EMPTY
    val solution = reply.solution ?: return TaskSolutions.EMPTY

    return TaskSolutions(lastSubmission.status.toCheckStatus(), solution.associate {
      @Suppress("RemoveExplicitTypeArguments") //it's required by compiler
      it.name to (it.text to emptyList<AnswerPlaceholder>())
    })
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

  override fun updateTasks(course: Course, tasks: List<Task>, progressIndicator: ProgressIndicator?) {
    super.updateTasks(course, tasks, progressIndicator)
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
  }
}

