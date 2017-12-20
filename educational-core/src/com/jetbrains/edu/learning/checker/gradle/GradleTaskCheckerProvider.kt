package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

abstract class GradleTaskCheckerProvider : TaskCheckerProvider {

  override fun getEduTaskChecker(task: EduTask, project: Project) = GradleEduTaskChecker(task, project)
  override fun getOutputTaskChecker(task: OutputTask, project: Project) = GradleOutputTaskChecker(task, project, this::mainClassForFile)
  override fun getTheoryTaskChecker(task: TheoryTask, project: Project) = GradleTheoryTaskChecker(task, project, this::mainClassForFile)

  abstract protected fun mainClassForFile(project: Project, file: VirtualFile): String?
}
