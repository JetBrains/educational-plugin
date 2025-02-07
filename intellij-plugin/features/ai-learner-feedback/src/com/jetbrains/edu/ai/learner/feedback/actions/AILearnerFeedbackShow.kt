package com.jetbrains.edu.ai.learner.feedback.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippySettings
import com.jetbrains.edu.ai.learner.feedback.grazie.AILearnerFeedbackGrazieClient
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class AILearnerFeedbackShow : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    currentThreadCoroutineScope().launch {
      val clippyProperties = AIClippySettings.getInstance().getClippySettings()
      val language = AIClippySettings.getInstance().language
      val feedback = AILearnerFeedbackGrazieClient.generateFeedback(clippyProperties, true).also { println(it) }
      AIClippyService.getInstance(project).showWithText(feedback)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = !AIClippyService.isActive(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.Student.AILearnerFeedbackShow"
  }
}