package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

/**
 * Action for changing the host for Edu AI service (it is used for AI-based features, such as AI hints and test generation)
 * For translations, see [TranslationServiceChangeHostAction])
 */
class EduAIServiceChangeHostAction : ChangeServiceHostAction<EduAIServiceHost>(EduAIServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.EduAIServiceChangeHost"
  }
}
