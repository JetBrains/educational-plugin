package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject

/**
 * The "place" for actions, if they are displayed in the balloon for Zhaba.
 */
const val ZHABA_SAYS_ACTION_PLACE = "ZhabaSaysPlace"

abstract class ZhabaActionBase : DumbAwareAction() {

  /**
   * Text shown inside the Zhaba balloon.
   */
  protected open val textToSay: String
    @NlsActions.ActionText get() = templateText

  override fun update(e: AnActionEvent) {
    val project = e.project

    e.presentation.isEnabledAndVisible = project != null && project.isEduProject()

    if (e.place == ZHABA_SAYS_ACTION_PLACE) {
      e.presentation.text = textToSay
      configureComponentForZhabaBalloon(e.presentation)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  /**
   * We use custom components because we want action buttons to look like buttons in the "GotIt" tooltip.
   */
  open fun configureComponentForZhabaBalloon(presentation: Presentation) {
    presentation.putClientProperty(ActionUtil.COMPONENT_PROVIDER, PrimaryButtonZhabaAction())
    presentation.putClientProperty(CustomComponentAction.ACTION_KEY, this@ZhabaActionBase)
  }
}
