package com.jetbrains.edu.ai.learner.feedback

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippySettings
import com.jetbrains.edu.ai.learner.feedback.grazie.AILearnerFeedbackGrazieClient
import com.jetbrains.edu.ai.learner.feedback.messages.EduAILearnerFeedbackBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class AILearnerFeedbackService(private val project: Project, private val scope: CoroutineScope) {
  fun showFeedbackInClippy(positive: Boolean) {
    scope.launch {
      val clippyProperties = AIClippySettings.getInstance().getClippySettings()
      val feedback = withBackgroundProgress(project, EduAILearnerFeedbackBundle.message("learner.feedback.calculating.feedback")) {
        AILearnerFeedbackGrazieClient.generateFeedback(clippyProperties, positive)
      }
      AIClippyService.getInstance(project).showWithText(feedback)
    }
  }

  suspend fun getFeedback(positive: Boolean): String {
    val clippyProperties = AIClippySettings.getInstance().getClippySettings()
    return withBackgroundProgress(project, EduAILearnerFeedbackBundle.message("learner.feedback.calculating.feedback")) {
      AILearnerFeedbackGrazieClient.generateFeedback(clippyProperties, positive)
    }
  }

  companion object {
    fun getInstance(project: Project): AILearnerFeedbackService = project.service()
  }
}