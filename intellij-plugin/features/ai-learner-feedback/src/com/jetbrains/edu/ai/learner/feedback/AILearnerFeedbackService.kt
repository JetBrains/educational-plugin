package com.jetbrains.edu.ai.learner.feedback

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippySettings
import com.jetbrains.edu.ai.learner.feedback.grazie.AILearnerFeedbackGrazieClient
import com.jetbrains.edu.ai.learner.feedback.messages.EduAILearnerFeedbackBundle

@Service(Service.Level.PROJECT)
class AILearnerFeedbackService(private val project: Project) {
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