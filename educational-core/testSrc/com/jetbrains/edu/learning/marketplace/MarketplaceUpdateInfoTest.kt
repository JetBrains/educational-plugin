package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector

class MarketplaceUpdateInfoTest : EduTestCase() {
  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureResponse(fileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      UPDATE_INFO_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse(fileName)
    }
  }

  fun `test course updateInfo loaded`() {
    configureResponse("updateInfo.json")
    val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(1)
    checkNotNull(updateInfo)
    assertEquals(102996, updateInfo.updateId)
    assertEquals(1, updateInfo.pluginId)
    assertEquals(3, updateInfo.version)
    assertEquals(13, updateInfo.compatibility.gte)
  }

  // course version from updateInfo.json should be incremented by 1
  fun `test remote course version set from updateInfo`() {
    configureResponse("updateInfo.json")
    val course = EduCourse()
    course.setRemoteMarketplaceCourseVersion()
    assertEquals(4, course.marketplaceCourseVersion)
  }

  fun `test remote course version not set for new course`() {
    configureResponse("emptyUpdateInfo.json")
    val course = EduCourse()
    course.setRemoteMarketplaceCourseVersion()
    assertEquals(0, course.marketplaceCourseVersion)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/updateInfo/"

  companion object {
    private val UPDATE_INFO_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}