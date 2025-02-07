package com.jetbrains.edu.ai.clippy.assistant.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.clippy.assistant.ClippyService
import com.jetbrains.edu.ai.clippy.assistant.grazie.ClippyGrazieClient
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippySettings
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ShowClippyAssistant : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    currentThreadCoroutineScope().launch {
      val clippyProperties = AIClippySettings.getInstance().getClippySettings()
      val feedback = ClippyGrazieClient.generateFeedback(clippyProperties).also { println(it) }
      ClippyService.getInstance(project).apply {
        showClippy()
        setClippyFeedback(feedback)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = !ClippyService.isActive(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.Student.ShowClippyAssistant"
  }
}