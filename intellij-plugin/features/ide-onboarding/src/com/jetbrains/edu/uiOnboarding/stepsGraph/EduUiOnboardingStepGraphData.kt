package com.jetbrains.edu.uiOnboarding.stepsGraph

class EduUiOnboardingStepGraphData(
  val isLast: Boolean,
  val stepId: String,
  stepIndex: Int?,
  totalSteps: Int
) : GotItBalloonGraphData(stepIndex, totalSteps)