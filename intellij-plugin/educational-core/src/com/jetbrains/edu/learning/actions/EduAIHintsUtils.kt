package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel.Companion.ACTION_PLACE
import org.jetbrains.annotations.NonNls

/**
 * @see [com.jetbrains.edu.aiHints.core]
 */
object EduAIHintsUtils {
  /**
   * @see [com.jetbrains.edu.aiHints.core.action.GetHint]
   */
  @NonNls
  const val GET_HINT_ACTION_ID: String = "Educational.Hints.GetHint"

  /**
   * Temporary solution because currently [com.jetbrains.edu.aiHints.core.action.GetHint]'s presentation is not propagated to corresponding
   * button in the [com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckDetailsPanel].
   *
   * @see <a href="https://youtrack.jetbrains.com/issue/EDU-7584">EDU-7584</a>
   */
  fun getHintActionPresentation(project: Project): GetHintActionPresentation {
    val action = ActionManager.getInstance().getAction(GET_HINT_ACTION_ID)
    // BACKCOMPAT: 2024.2 Replace with [AnActionEvent.createEvent]
    @Suppress("DEPRECATION", "removal")
    val anActionEvent = AnActionEvent.createFromInputEvent(
      null,
      ACTION_PLACE,
      action.templatePresentation.clone(),
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
    )
    runReadAction { ActionUtil.performDumbAwareUpdate(action, anActionEvent, false) }
    return when {
      anActionEvent.presentation.isEnabledAndVisible -> GetHintActionPresentation.ENABLED_AND_VISIBLE
      anActionEvent.presentation.isEnabled -> GetHintActionPresentation.ENABLED
      else -> GetHintActionPresentation.OTHER
    }
  }

  enum class GetHintActionPresentation {
    ENABLED_AND_VISIBLE, ENABLED, OTHER;

    fun isEnabledAndVisible(): Boolean = this == ENABLED_AND_VISIBLE

    fun isEnabled(): Boolean = this == ENABLED || this == ENABLED_AND_VISIBLE
  }
}