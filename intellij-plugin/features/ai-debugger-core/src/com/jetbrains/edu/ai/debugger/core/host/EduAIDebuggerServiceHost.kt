package com.jetbrains.edu.ai.debugger.core.host

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_SERVICE_URL
import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_STAGING_URL
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

enum class EduAIDebuggerServiceHost(private val visibleName: Supplier<@Nls String>, val url: String) {
  PRODUCTION(EduAIDebuggerCoreBundle.lazyMessage("ai.debugger.service.production.server"), EDU_AI_DEBUGGER_SERVICE_URL),
  STAGING(EduAIDebuggerCoreBundle.lazyMessage("ai.debugger.service.staging.server"), EDU_AI_DEBUGGER_STAGING_URL),
  OTHER(EduAIDebuggerCoreBundle.lazyMessage("ai.debugger.service.other"), "http://localhost:8080");

  override fun toString(): String = visibleName.get()

  companion object {
    @JvmStatic
    fun getSelectedHost(): EduAIDebuggerServiceHost {
      val selectedUrl = getSelectedUrl()
      return EduAIDebuggerServiceHost.values().firstOrNull { it.url == selectedUrl } ?: OTHER
    }

    @JvmStatic
    fun getSelectedUrl(): String = PropertiesComponent.getInstance().getValue(EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY, PRODUCTION.url)
  }
}