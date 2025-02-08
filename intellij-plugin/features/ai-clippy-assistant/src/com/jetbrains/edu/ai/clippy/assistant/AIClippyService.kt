package com.jetbrains.edu.ai.clippy.assistant

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.clippy.assistant.ui.AIClippyPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class AIClippyService(private val project: Project, private val scope: CoroutineScope) {
  @Volatile
  private var clippy: AIClippyPopup? = null
  private val lock = Mutex()

  fun showWithText(text: String) = showWithTextAndLinks(text, emptyList())

  fun showWithTextAndLinks(text: String, links: List<ClippyLinkAction>) {
    scope.launch {
      withContext(Dispatchers.EDT) {
        getClippy().apply {
          show(project)
          updateText(text)
          updateLinkActions(links)
        }
      }
    }
  }

  data class ClippyLinkAction(val name: String, val action: () -> Unit)

  private suspend fun getClippy(): AIClippyPopup =
    lock.withLock {
      if (clippy == null || clippy?.isDisposed == true) {
        clippy = AIClippyPopup()
      }
      clippy ?: error("Clippy is unexpectedly null")
    }

  companion object {
    fun getInstance(project: Project): AIClippyService = project.service()

    fun isActive(project: Project): Boolean = getInstance(project).clippy?.isVisible == true
  }
}