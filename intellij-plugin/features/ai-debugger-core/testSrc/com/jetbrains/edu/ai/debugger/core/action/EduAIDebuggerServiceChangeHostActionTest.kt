package com.jetbrains.edu.ai.debugger.core.action

import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost
import com.jetbrains.edu.learning.services.action.ServiceChangeHostActionTest
import org.jetbrains.annotations.NonNls

class EduAIServiceChangeHostActionTest : ServiceChangeHostActionTest() {
  override val actionId: String = EduAIDebuggerServiceChangeHost.ACTION_ID
  override val propertyName: String = EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY
  override var initialUrl: String = EduAIDebuggerServiceHost.PRODUCTION.url

  override val productionUrl: String = EduAIDebuggerServiceHost.PRODUCTION.url
  // TODO: change to staging url after staging is set up
  override val stagingUrl: String = "STAGING_URL"
  override val localhostUrl: String = LOCALHOST_URL

  override fun getSelectedUrl(): String = EduAIDebuggerServiceHost.getSelectedUrl()

  companion object {
    @NonNls
    private const val LOCALHOST_URL: String = "http://localhost:666/"
  }
}