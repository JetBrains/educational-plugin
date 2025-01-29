package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import org.jetbrains.annotations.TestOnly

/**
 * Keeps the current state of the user flow when learner interacts with AI-Hints feature.
 *
 * This helps decide what state of the UI learner should see at the moment.
 *
 * @see [com.jetbrains.edu.aiHints.core.action.GetHint]
 */
@Service(Service.Level.PROJECT)
class HintStateManager : EduTestAware {
  @Volatile
  private var state: HintState = HintState.DEFAULT

  private enum class HintState {
    DEFAULT,

    /**
     * Represents that the generated [com.jetbrains.educational.ml.hints.hint.CodeHint] was accepted by user
     *
     * @see [com.jetbrains.edu.aiHints.core.action.AcceptHint]
     */
    ACCEPTED;
  }

  fun reset() {
    state = HintState.DEFAULT
  }

  fun acceptHint() {
    state = HintState.ACCEPTED
  }

  @TestOnly
  override fun restoreState() {
    state = HintState.DEFAULT
  }

  companion object {
    fun isDefault(project: Project): Boolean = getInstance(project).state == HintState.DEFAULT

    fun getInstance(project: Project): HintStateManager = project.service()
  }
}