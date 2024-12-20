package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rider.model.RdProjectFolderCriterion
import com.jetbrains.rider.model.RdUnitTestCriterion
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities

class CSharpEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  CSharpEduTaskCheckerBase(task, envChecker, project) {
  override fun getRdUnitTestCriterion(): RdUnitTestCriterion {
    val virtualFileUrl = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
                           .findByUrl("${project.courseDir}/${task.pathInCourse}/${CSharpConfigurator.TEST_DIRECTORY}")
                         ?: error("No test directory found for task ${task.name}")

    val taskId = WorkspaceModel.getInstance(project).getProjectModelEntities(virtualFileUrl).firstOrNull()?.getId(project)
                 ?: error("No project model entity associated with task ${task.name} found")

    return RdProjectFolderCriterion(taskId)
  }
}