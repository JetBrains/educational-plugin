package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.aiHints.core.generator.AiCodeHintGenerator
import com.jetbrains.edu.aiHints.core.generator.AiTextHintGenerator
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.aiHints.core.ui.CodeHintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.ErrorHintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.TextHintInlineBanner
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@Service(Service.Level.PROJECT)
class HintsLoader(private val project: Project, private val scope: CoroutineScope) {

  fun getHint(task: Task) {
    scope.launch(Dispatchers.IO) {
      if (!mutex.tryLock()) {
        ErrorHintInlineBanner(project, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress"))
        return@launch
      }
      try {
        val taskProcessor = TaskProcessorImpl(task)
        val hintsAssistant = AiHintsAssistant.getAssistant(taskProcessor, AiCodeHintGenerator(taskProcessor), AiTextHintGenerator())
        val hint = withBackgroundProgress(project, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.progress.text"), cancellable = true) {
          hintsAssistant.getHint(taskProcessor.getSubmissionTextRepresentation() ?: "")
        }.getOrElse {
          ErrorHintInlineBanner(project, it.message ?: EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.error.unknown")) {
            getHint(task)
          }.display()
          return@launch
        }

        val codeHint = hint.codeHint
        if (codeHint != null) {
          val taskFile = taskProcessor.currentTaskFile ?: project.selectedTaskFile ?: error("Failed to obtain TaskFile")
          return@launch CodeHintInlineBanner(project, taskFile, hint.textHint.text, codeHint).display()
        }
        TextHintInlineBanner(project, hint.textHint.text).display()
      }
      finally {
        mutex.unlock()
      }
    }
  }

  private val mutex = Mutex()

  companion object {
    fun getInstance(project: Project): HintsLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}