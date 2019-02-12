package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.learning.EduUtils.isStudyProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSolutionLoader.Companion.IS_HYPERSKILL_SOLUTION_LOADING_STARTED
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillProjectComponent(private val project: Project) : ProjectComponent {

  override fun projectOpened() {
    if (project.isDisposed || !isStudyProject(project)) return

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      if (StudyTaskManager.getInstance(project).course !is HyperskillCourse) return@runWhenProjectIsInitialized
      val isSolutionLoadingStarted = IS_HYPERSKILL_SOLUTION_LOADING_STARTED.getRequired(project)
      if (HyperskillSettings.INSTANCE.account != null && !isSolutionLoadingStarted) {
        HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
  }
}
