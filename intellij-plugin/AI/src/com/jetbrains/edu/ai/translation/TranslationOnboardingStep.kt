package com.jetbrains.edu.ai.translation

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.use
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.action.AITranslation
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory.Companion.STUDY_TOOL_WINDOW
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import java.awt.Component
import java.awt.Point
import javax.swing.JComponent

class TranslationOnboardingStep : EduUiOnboardingStep {

  override fun performStep(project: Project, data: EduUiOnboardingAnimationData): GotItBalloonStepData? {
    val taskToolWindow = ToolWindowManager.getInstance(project).getToolWindow(STUDY_TOOL_WINDOW) ?: return null

    val actionButton = findActionButtonRecursively(
      // This is the implementation detail of where the toolbar with the translation button is located.
      // We should search it in the parent component of the tool window component.
      taskToolWindow.component.parent,
      ActionManager.getInstance().getAction(AITranslation.ACTION_ID)
    ) ?: return null

    val componentPoint = RelativePoint.getSouthOf(actionButton)

    val zhabaPoint = Point(
      componentPoint.originalPoint.x - ZHABA_DIMENSION.width / 2,
      componentPoint.originalPoint.y + expectedTooltipHeight()
    )

    val relativeZhabaPoint = RelativePoint(actionButton, zhabaPoint)
    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint)

    return GotItBalloonStepData(gotItTooltipBuilder(), componentPoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  private fun findActionButtonRecursively(component: Component?, targetAction: AnAction): ActionButton? {
    if (component == null) return null

    if (component is ActionButton && component.action == targetAction) {
      return component
    }

    if (component is JComponent) {
      for (child in component.components) {
        val found = findActionButtonRecursively(child, targetAction)
        if (found != null) return found
      }
    }

    return null
  }

  private fun gotItTooltipBuilder(): GotItComponentBuilder = GotItComponentBuilder { EduAIBundle.message("onboarding.step.translation.text") }
    .withHeader(EduAIBundle.message("onboarding.step.translation.header"))

  private fun expectedTooltipHeight(): Int = Disposer.newDisposable().let { disposable ->
    disposable.use {
      // There is a very thick invisible border around the Got-it tooltip used for shadowing,
      // we should substitute it from its preferred height.
      gotItTooltipBuilder().build(disposable).preferredSize.height - Registry.intValue("ide.balloon.shadow.size")
    }
  }

  override fun buildAnimation(
    data: EduUiOnboardingAnimationData,
    point: RelativePoint
  ): EduUiOnboardingAnimation = object : EduUiOnboardingAnimation {
    override val steps: List<EduUiOnboardingAnimationStep> = listOf(
      EduUiOnboardingAnimationStep(data.lookUp, point, point, 3_000),
    )

    override val cycle: Boolean = true
  }

  companion object {
    const val STEP_KEY = "translation"
  }
}