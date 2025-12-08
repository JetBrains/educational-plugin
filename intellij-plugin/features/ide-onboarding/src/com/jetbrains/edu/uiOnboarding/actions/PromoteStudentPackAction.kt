package com.jetbrains.edu.uiOnboarding.actions

import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph

class PromoteStudentPackAction : ZhabaActionBase() {
  override val appearanceStepId: String = ZhabaMainGraph.STEP_ID_PROMOTE_STUDENT_PACK_JUMP_OUT

  companion object {
    const val ACTION_ID: String = "Educational.StartNewUiOnboardingAction"
  }
}