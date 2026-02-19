package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.host.TranslationServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class TranslationServiceChangeHostAction : ChangeServiceHostAction<TranslationServiceHost>(TranslationServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.TranslationServiceChangeHost"
  }
}