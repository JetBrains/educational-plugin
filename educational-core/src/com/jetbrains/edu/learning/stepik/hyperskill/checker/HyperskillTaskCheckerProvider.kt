package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener

class HyperskillTaskCheckerProvider(private val baseProvider: TaskCheckerProvider) : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = baseProvider.codeExecutor

  override val envChecker: EnvironmentChecker
    get() = baseProvider.envChecker

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    val checker = baseProvider.getEduTaskChecker(task, project)

    return object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = checker.check(indicator)
        val course = task.course as HyperskillCourse
        val resultingStatus = checkResult.status
        if (resultingStatus == CheckStatus.Solved) {
          val projectLesson = course.getProjectLesson() ?: return CheckResult(resultingStatus, CheckUtils.CONGRATULATIONS)
          val otherUnsolvedTasks = projectLesson.taskList.filter { it != task && it.status != CheckStatus.Solved }
          return if (otherUnsolvedTasks.isEmpty())
            CheckResult(resultingStatus,
                        EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL),
                        hyperlinkListener = EduBrowserHyperlinkListener.INSTANCE)
          else CheckResult(resultingStatus, CheckUtils.CONGRATULATIONS)
        }
        return checkResult
      }

      override fun onTaskSolved() = checker.onTaskSolved()
      override fun onTaskFailed() = checker.onTaskFailed()
      override fun clearState() = checker.clearState()
    }
  }

  override fun getOutputTaskChecker(
    task: OutputTask,
    project: Project,
    codeExecutor: CodeExecutor
  ): OutputTaskChecker = baseProvider.getOutputTaskChecker(task, project, codeExecutor)

  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker =
    baseProvider.getTheoryTaskChecker(task, project)

  override fun getChoiceTaskChecker(task: ChoiceTask, project: Project): TaskChecker<ChoiceTask>? =
    baseProvider.getChoiceTaskChecker(task, project)

  override fun getCodeTaskChecker(task: CodeTask, project: Project): TaskChecker<CodeTask>? =
    baseProvider.getCodeTaskChecker(task, project)

  override fun getIdeTaskChecker(task: IdeTask, project: Project): TaskChecker<IdeTask> =
    baseProvider.getIdeTaskChecker(task, project)
}