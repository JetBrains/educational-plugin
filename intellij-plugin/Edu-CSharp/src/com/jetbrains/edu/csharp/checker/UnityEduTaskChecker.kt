package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rider.model.RdProjectFileCriterion
import com.jetbrains.rider.model.RdUnitTestCriterion
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities

class UnityEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  CSharpEduTaskCheckerBase(task, envChecker, project) {
  override fun getRdUnitTestCriterion(): RdUnitTestCriterion {
    val virtualFileUrl = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
                           .findByUrl("${project.courseDir}/$PLAY_MODE_DIR/$TASK/${CSharpConfigurator.TEST_CS}")
                         ?: error("No virtual file url for ${CSharpConfigurator.TEST_CS} file found")
    val taskId = WorkspaceModel.getInstance(project).getProjectModelEntities(virtualFileUrl).firstOrNull()?.getId(project)
                 ?: error("No project model entity associated with test file ${CSharpConfigurator.TEST_CS} found")

    return RdProjectFileCriterion(taskId)
  }

  companion object {
    private const val PLAY_MODE_DIR: String = "PlayMode"
  }
}