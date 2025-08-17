package com.jetbrains.edu.ai.host

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.ai.EDU_AI_SERVICE_PRODUCTION_URL
import com.jetbrains.edu.ai.EDU_AI_SERVICE_STAGING_URL
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

@Suppress("unused") // All enum values ar used in UI
enum class EduAIServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(EDU_AI_SERVICE_PRODUCTION_URL, "change.service.host.production"),
  STAGING(EDU_AI_SERVICE_STAGING_URL, "change.service.host.staging"),
  OTHER("http://localhost:8080", "change.service.host.other");

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<EduAIServiceHost>("AI service", EduAIServiceHost::class.java) {
    override val default: EduAIServiceHost = PRODUCTION
    override val other: EduAIServiceHost = OTHER
  }
}