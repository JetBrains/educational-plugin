package com.jetbrains.edu.learning.fullLine

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

// BACKCOMPAT: 2025.3: inline
fun disableAiCompletion(project: Project) {
  project.service<MLCompletionPerProjectSuppressor>().suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy"))
}

fun enableAiCompletion(project: Project) {
  project.service<MLCompletionPerProjectSuppressor>().unsuppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy"))
}
