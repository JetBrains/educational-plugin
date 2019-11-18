package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.handlers.CodeExecutor

abstract class GradleTaskCheckerProvider : TaskCheckerProvider {

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = NewGradleEduTaskChecker(task, project)
  override fun getOutputTaskChecker(task: OutputTask, project: Project, codeExecutor: CodeExecutor): OutputTaskChecker =
    GradleOutputTaskChecker(task, project, codeExecutor, this::mainClassForFile)
  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker =
    GradleTheoryTaskChecker(task, project, this::mainClassForFile)

  protected abstract fun mainClassForFile(project: Project, file: VirtualFile): String?
}
