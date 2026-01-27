package com.jetbrains.edu.uiOnboarding.steps.tour

import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.actions.CallTodeAction
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import kotlinx.coroutines.CoroutineScope


data class OnboardingLastStepData(val project: Project) : ZhabaData

class OnboardingLastStep internal constructor(
  override val stepId: String,
) : ZhabaStep<OnboardingLastStepData, GraphData.EMPTY> {

  override fun performStep(project: Project, data: EduUiOnboardingAnimationData): OnboardingLastStepData = OnboardingLastStepData(project)

  override suspend fun executeStep(stepData: OnboardingLastStepData, graphData: GraphData.EMPTY, cs: CoroutineScope, disposable: Disposable): String {
    NotificationGroupManager
      .getInstance()
      .getNotificationGroup("EduOnboarding")
      .createNotification(EduUiOnboardingBundle.message("finished.reminder", getMenuPath()), MessageType.INFO)
      .notify(stepData.project)

    return FINISH_TRANSITION
  }

  private fun getMenuPath(): String {
    val helpAction = ActionManager.getInstance().getAction(IdeActions.GROUP_HELP_MENU)
    val zhabaAction = ActionManager.getInstance().getAction(CallTodeAction.ACTION_ID) as? CallTodeAction

    val helpName = helpAction.templatePresentation.text ?: ""
    val actionName = zhabaAction?.actionName() ?: ""

    return "$helpName > $actionName"
  }
}