package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

class TextHintInlineBanner(
  project: Project,
  message: @Nls String,
) : HintInlineBanner(project, message)
