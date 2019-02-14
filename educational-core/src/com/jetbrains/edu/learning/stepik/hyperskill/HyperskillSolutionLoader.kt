package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.api.Submission

class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override fun provideTasksToUpdate(course: Course): List<Task> {
    return course.items.asSequence().flatMap {
      when (it) {
        is Lesson -> sequenceOf(it)
        is Section -> it.items.asSequence().filterIsInstance<Lesson>()
        else -> emptySequence()
      }
    }.flatMap { it.taskList.asSequence() }.toList()
  }

  override fun loadLastSubmission(stepId: Int): Submission? = HyperskillConnector.getSubmission(stepId)

  override fun loadSolution(task: Task): TaskSolutions {
    task.course.putUserData(IS_HYPERSKILL_SOLUTION_LOADING_STARTED, true)
    return super.loadSolution(task)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillSolutionLoader = ServiceManager.getService(project, HyperskillSolutionLoader::class.java)

    val IS_HYPERSKILL_SOLUTION_LOADING_STARTED: Key<Boolean> = KeyWithDefaultValue.create("IS_HYPERSKILL_SOLUTION_LOADING_STARTED", false)
  }
}

