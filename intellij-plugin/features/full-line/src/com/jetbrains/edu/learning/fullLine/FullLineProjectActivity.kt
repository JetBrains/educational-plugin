package com.jetbrains.edu.learning.fullLine

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.StudyTaskManager

class FullLineProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    updateAiCompletion(project, course)
  }
}
