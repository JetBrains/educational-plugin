package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

abstract class GradleTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = GradleEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    NewGradleEduTaskChecker(task, envChecker, project)

  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker =
    GradleTheoryTaskChecker(task, project, this::mainClassForFile)

  abstract fun mainClassForFile(project: Project, file: VirtualFile): String?

  override fun getCodeExecutor(): CodeExecutor = GradleCodeExecutor(this::mainClassForFile)
}
