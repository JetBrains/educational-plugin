package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import org.jetbrains.annotations.Nls

class ErrorHintInlineBanner(
  project: Project,
  message: @Nls String,
  retryAction: Runnable? = null
) : HintInlineBanner(project, message, Status.Error) {
  init {
    if (retryAction != null) {
      addAction(EduAIHintsCoreBundle.message("hints.label.retry")) {
        close()
        retryAction.run()
      }
    }
  }
}