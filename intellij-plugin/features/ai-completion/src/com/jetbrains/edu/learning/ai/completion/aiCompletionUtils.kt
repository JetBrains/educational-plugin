package com.jetbrains.edu.learning.ai.completion

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature

fun updateAiCompletion(project: Project, course: Course) {
  if (course.disabledFeatures.contains(EduManagedFeature.AI_COMPLETION.featureKey)) {
    disableAiCompletion(project)
  }
  else {
    enableAiCompletion(project)
  }
}
