package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskChecker
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskWithFileIOTaskChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask

/**
 * If you add any new methods here, please do not forget to add it also to
 * @see com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
 */
interface TaskCheckerProvider {
  val codeExecutor: CodeExecutor
    get() = DefaultCodeExecutor()

  val envChecker: EnvironmentChecker
    get() = EnvironmentChecker()

  fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>

  fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker = TheoryTaskChecker(task, project)

  // Should not be overloaded by anyone
  fun getTaskChecker(task: Task, project: Project): TaskChecker<*>? {
    return when (task) {
      is RemoteEduTask,
      is StringTask, is NumberTask, is CodeTask, is DataTask -> null

      is EduTask -> getEduTaskChecker(task, project)
      is OutputTask -> OutputTaskChecker(task, envChecker, project, codeExecutor)
      is TheoryTask -> getTheoryTaskChecker(task, project)
      is ChoiceTask -> if (task.canCheckLocally) ChoiceTaskChecker(task, project) else null
      is IdeTask -> IdeTaskChecker(task, project)
      is CodeforcesTaskWithFileIO -> CodeforcesTaskWithFileIOTaskChecker(task, project)
      is CodeforcesTask -> CodeforcesTaskChecker(task, envChecker, project, codeExecutor)
      else -> throw IllegalStateException("Unknown task type: " + task.itemType)
    }
  }
}