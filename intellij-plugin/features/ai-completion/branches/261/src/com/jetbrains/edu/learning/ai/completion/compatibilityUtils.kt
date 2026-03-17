package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.project.Project

// BACKCOMPAT: 2025.3: inline
fun disableAiCompletion(project: Project) {
  if (MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return
  MLCompletionPerProjectSuppressor.getInstance(project).suppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
}

// BACKCOMPAT: 2025.3: inline
fun enableAiCompletion(project: Project) {
  if (!MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed()) return
  MLCompletionPerProjectSuppressor.getInstance(project).unsuppress(AI_COMPLETION_SUPPRESSOR_TOKEN)
}

private val AI_COMPLETION_SUPPRESSOR_TOKEN = MLCompletionPerProjectSuppressor.Token("JetBrains Academy")