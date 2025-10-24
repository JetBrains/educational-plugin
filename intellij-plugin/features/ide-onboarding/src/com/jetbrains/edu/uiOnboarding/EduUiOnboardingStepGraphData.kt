package com.jetbrains.edu.uiOnboarding

import com.jetbrains.edu.uiOnboarding.steps.GotItBalloonGraphData

class EduUiOnboardingStepGraphData(
  val isLast: Boolean,
  val stepId: String,
  stepIndex: Int?,
  totalSteps: Int
) : GotItBalloonGraphData(stepIndex, totalSteps)