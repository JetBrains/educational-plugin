package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
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
   * Temporary solution because currently [com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckDetailsPanel]'s content depends on the
   * presentation of the [com.jetbrains.edu.aiHints.core.action.GetHint] action.
   */
  fun getHintActionPresentation(project: Project): GetHintActionPresentation {
    val action = ActionManager.getInstance().getAction(GET_HINT_ACTION_ID)
    val anActionEvent = AnActionEvent.createEvent(
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build(),
      action.templatePresentation.clone(),
      ACTION_PLACE,
      ActionUiKind.NONE,
      null
    )
    runReadAction { ActionUtil.performDumbAwareUpdate(action, anActionEvent, false) }
    return when {
      anActionEvent.presentation.isEnabled -> GetHintActionPresentation.ENABLED
      else -> GetHintActionPresentation.OTHER
    }
  }

  enum class GetHintActionPresentation {
    ENABLED, OTHER;

    fun isEnabled(): Boolean = this == ENABLED
  }
}