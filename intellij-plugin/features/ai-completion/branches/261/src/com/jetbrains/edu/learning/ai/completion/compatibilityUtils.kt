package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.project.Project

// BACKCOMPAT: 2025.3: inline
fun disableAiCompletion(project: Project) {
  if (MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return
  MLCompletionPerProjectSuppressor.getInstance(project).suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy"))
}

fun enableAiCompletion(project: Project) {
  if (!MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return
  MLCompletionPerProjectSuppressor.getInstance(project).unsuppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy"))
}
