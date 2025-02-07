package com.jetbrains.edu.ai.clippy.assistant

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.clippy.assistant.grazie.ClippyGrazieClient
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.ai.clippy.assistant.settings.AIClippySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class ClippyService(private val project: Project, private val scope: CoroutineScope) {
  @Volatile
  private var clippy: Clippy? = null
  private val lock = Mutex()

  suspend fun showClippy() {
    withContext(Dispatchers.EDT) {
      getClippy().show(project)
    }
  }

  suspend fun setClippyFeedback(text: String) {
    withContext(Dispatchers.EDT) {
      getClippy().updateText(text)
    }
  }

  fun showWithFeedback() {
    scope.launch {
      val clippyProperties = AIClippySettings.getInstance().getClippySettings()
      val feedback = withBackgroundProgress(project, EduAIClippyAssistantBundle.message("clippy.calculating.feedback")) {
        ClippyGrazieClient.generateFeedback(clippyProperties)
      }
      withContext(Dispatchers.EDT) {
        getClippy().apply {
          show(project)
          updateText(feedback)
        }
      }
    }
  }

  private suspend fun getClippy(): Clippy =
    lock.withLock {
      if (clippy == null || clippy?.isDisposed == true) {
        clippy = Clippy(project)
      }
      clippy ?: error("Clippy is unexpectedly null")
    }

  companion object {
    fun getInstance(project: Project): ClippyService = project.service()

    fun isActive(project: Project): Boolean = getInstance(project).clippy?.isVisible == true
  }
}