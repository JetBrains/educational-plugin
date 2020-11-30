package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase

class MarketplaceDecodeHubTokenTest : EduTestCase() {

  fun `test decode hub token`() {
    val accessToken = "1606469838449.5273fef1-d3d6-4cea-822f-75ed72cab637.userId-4137-4f44-b039-3565991userId.5273fef1-d3d6-4cea-822f-75ed" +
                      "72cab637;1.MCwCFGGwUf5xgxmZZFOfd3GVm+TBXztNAhRnU0EdliAnbtSEEKg3c2lGmzTlXg=="
    val expectedUserId = "userId-4137-4f44-b039-3565991userId"
    assertEquals(expectedUserId, decodeHubToken(accessToken))
  }

  fun `test decode empty hub token`() {
    val accessToken = "1606469838449.5273fef1-d3d6-4cea-822f-75ed72cab637..5273fef1-d3d6-4cea-822f-75ed72cab637;1.MCwCFGGwUf5xgxmZZFOfd3GVm+TBXztNAhRnU0EdliAnbtSEEKg3c2lGmzTlXg=="
    assertNull(decodeHubToken(accessToken))
  }
}