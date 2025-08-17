package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class EduAIServiceChangeHostAction : ChangeServiceHostAction<EduAIServiceHost>(EduAIServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.EduAIServiceChangeHost"
  }
}
