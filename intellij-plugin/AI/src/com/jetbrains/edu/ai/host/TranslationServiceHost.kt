package com.jetbrains.edu.ai.host

import com.intellij.ide.Region
import com.intellij.ide.RegionSettings
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.ai.EDU_AI_SERVICE_PRODUCTION_CHINA_URL
import com.jetbrains.edu.ai.EDU_AI_SERVICE_PRODUCTION_URL
import com.jetbrains.edu.ai.EDU_AI_SERVICE_STAGING_URL
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

@Suppress("unused") // All enum values ar used in UI
enum class TranslationServiceHost(
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION("change.service.host.production") {
    override val url: String = EDU_AI_SERVICE_PRODUCTION_URL
  },
  STAGING("change.service.host.staging") {
    override val url: String = EDU_AI_SERVICE_STAGING_URL
  },
  OTHER("change.service.host.other") {
    override val url: String = "http://localhost:8082"
  };

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<TranslationServiceHost>("Translation service", TranslationServiceHost::class.java) {
    override val default: TranslationServiceHost = PRODUCTION
    override val other: TranslationServiceHost = OTHER
  }
}