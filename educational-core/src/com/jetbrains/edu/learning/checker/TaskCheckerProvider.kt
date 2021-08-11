package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskChecker
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskWithFileIOTaskChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask

/**
 * If you add any new methods here, please do not forget to add it also to
 * @see com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTaskCheckerProvider
 */
interface TaskCheckerProvider {
  val codeExecutor: CodeExecutor
    get() = DefaultCodeExecutor()

  val envChecker: EnvironmentChecker
    get() = EnvironmentChecker()

  fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>

  fun getOutputTaskChecker(task: OutputTask, project: Project, codeExecutor: CodeExecutor): OutputTaskChecker {
    return OutputTaskChecker(task, envChecker, project, codeExecutor)
  }

  fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker = TheoryTaskChecker(task, project)

  fun getChoiceTaskChecker(task: ChoiceTask, project: Project): TaskChecker<ChoiceTask>? {
    return if (task.canCheckLocally) ChoiceTaskChecker(task, project) else null
  }

  fun getCodeTaskChecker(task: CodeTask, project: Project): TaskChecker<CodeTask>? = null

  fun getIdeTaskChecker(task: IdeTask, project: Project): TaskChecker<IdeTask> = IdeTaskChecker(task, project)

  fun getDataTaskChecker(task: DataTask, project: Project): TaskChecker<DataTask>? = null

  // Should not be overloaded by anyone
  fun getTaskChecker(task: Task, project: Project): TaskChecker<*>? {
    return when (task) {
      is EduTask -> getEduTaskChecker(task, project)
      is OutputTask -> getOutputTaskChecker(task, project, codeExecutor)
      is TheoryTask -> getTheoryTaskChecker(task, project)
      is DataTask -> getDataTaskChecker(task, project)
      is CodeTask -> getCodeTaskChecker(task, project)
      is ChoiceTask -> getChoiceTaskChecker(task, project)
      is IdeTask -> getIdeTaskChecker(task, project)
      is CodeforcesTaskWithFileIO -> CodeforcesTaskWithFileIOTaskChecker(task, project)
      is CodeforcesTask -> CodeforcesTaskChecker(task, envChecker, project, codeExecutor)
      else -> throw IllegalStateException("Unknown task type: " + task.itemType)
    }
  }
}