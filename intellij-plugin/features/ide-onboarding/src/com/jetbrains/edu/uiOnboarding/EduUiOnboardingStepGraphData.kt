package com.jetbrains.edu.uiOnboarding

class EduUiOnboardingStepGraphData(
  val isLast: Boolean,
  val stepId: String,
  stepIndex: Int?,
  totalSteps: Int
) : GotItBalloonGraphData(stepIndex, totalSteps)