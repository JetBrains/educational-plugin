package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector

class MarketplaceLoadUpdateIdTest : EduTestCase() {
  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      UPDATE_ID_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("updateId.json")
    }
  }

  fun `test course updateId loaded`() {
    configureResponse()
    val updateId = MarketplaceConnector.getInstance().getLatestCourseUpdateId(1)
    assertEquals(102996, updateId)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/loadCourses/"

  companion object {
    private val UPDATE_ID_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}