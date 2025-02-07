package com.jetbrains.edu.ai.clippy.assistant.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.clippy.assistant.ClippyDiffService
import com.jetbrains.edu.ai.clippy.assistant.ClippyService
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls

class ShowDiffClippyComments : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    currentThreadCoroutineScope().launch {
      ClippyDiffService.getInstance(project).getClippyComments()
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = ClippyService.isActive(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    const val ACTION_ID: String = "Educational.Student.ShowDiffClippyComments"
  }
}