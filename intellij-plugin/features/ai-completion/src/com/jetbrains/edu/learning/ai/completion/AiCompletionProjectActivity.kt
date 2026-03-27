package com.jetbrains.edu.learning.ai.completion

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode

class AiCompletionProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val course = project.course ?: return
    if (course.courseMode == CourseMode.EDUCATOR) return

    project.service<AiCompletionFeatureWatcher>().observeAiCompletionFeature(course)
  }
}
