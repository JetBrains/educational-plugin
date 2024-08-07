package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.SolutionLoaderBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.Companion.toCheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.CodeTaskReply
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.markStageAsCompleted
import com.jetbrains.edu.learning.stepik.hyperskill.openSelectedStage
import com.jetbrains.edu.learning.submissions.Submission

@Service(Service.Level.PROJECT)
class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    // submission.taskId can differ from task.id because some hyperskill submissions were stored on stepik and got stepik step ID instead
    // of hyperskill task ID, see EDU-5186
    val lastSubmission: Submission = submissions.firstOrNull { if (!task.course.isStudy) true else it.taskId == task.id }
                                     ?: return TaskSolutions.EMPTY
    if (lastSubmission !is StepikBasedSubmission)
      error(
        "Hyperskill submission ${lastSubmission.id} for task ${task.name} is not instance of ${StepikBasedSubmission::class.simpleName} class")

    val files: Map<String, Solution> = when (task) {
      is EduTask -> lastSubmission.eduTaskFiles
      is CodeTask -> lastSubmission.codeTaskFiles(task)
      is SortingTask, is MatchingTask, is ChoiceTask, is TableTask, is UnsupportedTask -> emptyMap()
      else -> {
        LOG.warn("Solutions for task ${task.name} of type ${task::class.simpleName} not loaded")
        emptyMap()
      }
    }.filter { (_, solution) -> solution.isVisible }

    return TaskSolutions(lastSubmission.time, lastSubmission.status?.toCheckStatus() ?: CheckStatus.Unchecked, files)
  }

  private val StepikBasedSubmission.eduTaskFiles: Map<String, Solution>
    get() = solutionFiles?.associate { it.name to Solution(it.text, it.isVisible, emptyList()) } ?: emptyMap()

  private fun StepikBasedSubmission.codeTaskFiles(task: CodeTask): Map<String, Solution> {
    val codeFromServer = (reply as? CodeTaskReply)?.code ?: return emptyMap()
    val configurator = task.course.configurator as? HyperskillConfigurator ?: return emptyMap()
    val taskFile = configurator.getCodeTaskFile(project, task) ?: return emptyMap()
    return mapOf(taskFile.name to Solution(codeFromServer, true, emptyList()))
  }

  override fun updateTasks(course: Course,
                           tasks: List<Task>,
                           submissions: List<Submission>,
                           progressIndicator: ProgressIndicator?,
                           force: Boolean) {
    super.updateTasks(course, tasks, submissions, progressIndicator, force)
    runInEdt {
      progressIndicator?.text = EduCoreBundle.message("update.setting.stage")
      openSelectedStage(course, project)
    }
  }

  override fun updateTask(project: Project, task: Task, submissions: List<Submission>, force: Boolean): Boolean {
    val course = task.course as HyperskillCourse
    if (course.isStudy && task.lesson == course.getProjectLesson() && submissions.any { it.taskId == task.id && it.status == CORRECT }) {
      markStageAsCompleted(task)
    }
    return super.updateTask(project, task, submissions, force)
  }

  companion object {
    fun getInstance(project: Project): HyperskillSolutionLoader = project.service()

    private val LOG = Logger.getInstance(HyperskillSolutionLoader::class.java)
  }
}

