package com.jetbrains.edu.learning.marketplace.api

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceChangeHost
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceChangeHostUI
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.marketplace.changeHost.withMockSubmissionsServiceChangeHostUI
import com.jetbrains.edu.learning.testAction
import org.jetbrains.annotations.NonNls

class SubmissionsServiceChangeHostActionTest : EduTestCase() {
  private var initialUrl: String = SubmissionsServiceHost.PRODUCTION.url

  override fun setUp() {
    super.setUp()
    initialUrl = SubmissionsServiceHost.getSelectedUrl()
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, initialUrl, SubmissionsServiceHost.PRODUCTION.url)
    } finally {
      super.tearDown()
    }
  }

  fun `test change production to staging host`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.PRODUCTION.url, SubmissionsServiceHost.STAGING.url)

  fun `test change back from staging to production`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.STAGING.url, SubmissionsServiceHost.PRODUCTION.url)

  fun `test change production to localhost`() =
    doTestSubmissionsServiceChanged(SubmissionsServiceHost.PRODUCTION.url, LOCALHOST_URL)

  fun `test change back from localhost to production`() =
    doTestSubmissionsServiceChanged(LOCALHOST_URL, SubmissionsServiceHost.PRODUCTION.url)

  fun `test change staging to localhost`() = doTestSubmissionsServiceChanged(SubmissionsServiceHost.STAGING.url, LOCALHOST_URL)

  fun `test change back from localhost to staging`() = doTestSubmissionsServiceChanged(LOCALHOST_URL, SubmissionsServiceHost.STAGING.url)

  private fun doTestSubmissionsServiceChanged(initialHost: String, newHost: String) {
    PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, initialHost)
    assertEquals(initialHost, SubmissionsServiceHost.getSelectedUrl())
    doSubmissionsServiceChangeHostAction(newHost)
    assertEquals(newHost, SubmissionsServiceHost.getSelectedUrl())
  }

  private fun doSubmissionsServiceChangeHostAction(url: String) {
    withMockSubmissionsServiceChangeHostUI(object : SubmissionsServiceChangeHostUI {
      override fun getResultUrl(): String = url
    }) {
      testAction(SubmissionsServiceChangeHost.ACTION_ID)
    }
  }

  companion object {
    @NonNls
    private const val LOCALHOST_URL = "http://localhost:666/"
  }
}