package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.openSelectedStage

class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override val loadingTopic: Topic<SolutionLoadingListener> = SOLUTION_TOPIC

  override fun provideTasksToUpdate(course: Course): List<Task> {
    return course.items.asSequence().flatMap {
      when (it) {
        is Lesson -> sequenceOf(it)
        is Section -> it.items.asSequence().filterIsInstance<Lesson>()
        else -> emptySequence()
      }
    }.flatMap { it.taskList.asSequence() }.toList()
  }

  override fun loadLastSubmission(stepId: Int): Submission? = HyperskillConnector.getInstance().getSubmission(stepId)

  override fun loadSolution(task: Task): TaskSolutions {
    task.course.putUserData(IS_HYPERSKILL_SOLUTION_LOADING_STARTED, true)
    return super.loadSolution(task)
  }

  override fun updateTasks(course: Course, tasks: List<Task>, progressIndicator: ProgressIndicator?) {
    super.updateTasks(course, tasks, progressIndicator)
    runInEdt {
      openSelectedStage(course, project)
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillSolutionLoader = ServiceManager.getService(project, HyperskillSolutionLoader::class.java)

    val IS_HYPERSKILL_SOLUTION_LOADING_STARTED: Key<Boolean> = KeyWithDefaultValue.create("IS_HYPERSKILL_SOLUTION_LOADING_STARTED", false)

    val SOLUTION_TOPIC: Topic<SolutionLoadingListener> = Topic.create("Hyperskill solutions loaded", SolutionLoadingListener::class.java)
  }
}

