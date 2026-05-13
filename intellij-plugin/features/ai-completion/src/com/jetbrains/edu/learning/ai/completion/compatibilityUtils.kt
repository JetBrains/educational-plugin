package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

// BACKCOMPAT: 2025.3: inline
fun disableAiCompletion(project: Project, course: Course) {
  if (course.courseMode == CourseMode.EDUCATOR) return
  if (MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return

  MLCompletionPerProjectSuppressor.getInstance(project).suppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
}

// BACKCOMPAT: 2025.3: inline
fun enableAiCompletion(project: Project, course: Course) {
  if (course.courseMode == CourseMode.EDUCATOR) return
  if (!MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return

  MLCompletionPerProjectSuppressor.getInstance(project).unsuppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
}

internal val AI_COMPLETION_SUPPRESSOR_TOKEN = MLCompletionPerProjectSuppressor.Token("JetBrains Academy")