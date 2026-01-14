package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService

class HideTodeAction : ZhabaActionBase() {

  override val textToSay: String = EduUiOnboardingBundle.message("action.Educational.HideTodeAction.zhaba.text")

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    EduUiOnboardingService.getInstance(project).hideTode()
  }

  override fun configureComponentForZhabaBalloon(presentation: Presentation) {
    super.configureComponentForZhabaBalloon(presentation)
    presentation.putClientProperty(ActionUtil.COMPONENT_PROVIDER, SecondaryButtonZhabaAction())
  }

  override fun update(e: AnActionEvent) {
    super.update(e)

    val project = e.project ?: return

    if (!EduUiOnboardingService.getInstance(project).tourInProgress) {
      e.presentation.isEnabledAndVisible = false
    }
  }
}