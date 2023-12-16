package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

open class CppTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = CppCodeExecutor()

  // TODO implement envChecker validation
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppEduTaskChecker(task, envChecker, project)
  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker = CppTheoryTaskChecker(task, project)
}

class CppGTaskCheckerProvider : CppTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppGEduTaskChecker(task, envChecker, project)
}

class CppCatchTaskCheckerProvider : CppTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppCatchEduTaskChecker(task, envChecker, project)
}
