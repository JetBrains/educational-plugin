package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rider.plugins.unity.isUnityProject

class CSharpTaskCheckerProvider : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): CSharpEduTaskCheckerBase =
    if (!project.isUnityProject.value) {
      CSharpEduTaskChecker(task, envChecker, project)
    }
    else {
      UnityEduTaskChecker(task, envChecker, project)
    }
}