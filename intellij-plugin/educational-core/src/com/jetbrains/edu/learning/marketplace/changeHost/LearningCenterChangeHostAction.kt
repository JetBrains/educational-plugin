package com.jetbrains.edu.learning.marketplace.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class LearningCenterChangeHostAction : ChangeServiceHostAction<LearningCenterServiceHost>(LearningCenterServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.LearningCenterChangeHost"
  }
}
