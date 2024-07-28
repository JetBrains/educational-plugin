package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceChangeHost
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.services.action.ServiceChangeHostActionTest
import org.jetbrains.annotations.NonNls

class SubmissionsServiceChangeHostActionTest : ServiceChangeHostActionTest() {
  override val actionId: String = SubmissionsServiceChangeHost.ACTION_ID
  override val propertyName: String = SUBMISSIONS_SERVICE_HOST_PROPERTY
  override var initialUrl: String = SubmissionsServiceHost.PRODUCTION.url

  override val productionUrl: String = SubmissionsServiceHost.PRODUCTION.url
  override val stagingUrl: String = SubmissionsServiceHost.STAGING.url
  override val localhostUrl: String = LOCALHOST_URL

  override fun getSelectedUrl(): String = SubmissionsServiceHost.getSelectedUrl()

  companion object {
    @NonNls
    private const val LOCALHOST_URL = "http://localhost:666/"
  }
}