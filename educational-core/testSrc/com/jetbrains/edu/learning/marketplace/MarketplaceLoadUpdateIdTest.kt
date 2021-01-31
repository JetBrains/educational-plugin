package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector

class MarketplaceLoadUpdateInfoTest : EduTestCase() {
  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      UPDATE_ID_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("updateInfo.json")
    }
  }

  fun `test course updateInfo loaded`() {
    configureResponse()
    val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(1)
    checkNotNull(updateInfo)
    assertEquals(102996, updateInfo.updateId)
    assertEquals(3, updateInfo.version)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/loadCourses/"

  companion object {
    private val UPDATE_ID_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}