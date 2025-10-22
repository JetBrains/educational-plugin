package com.jetbrains.edu.uiOnboarding

import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData

data class EduUiOnboardingStepGraphData(
  val isLast: Boolean,
  val stepId: String,
  val stepIndex: Int?,
  val totalSteps: Int
) : GraphData