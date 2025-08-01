package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import org.junit.Test

class MarketplaceUpdateInfoTest : EduTestCase() {
  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureResponse(fileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      UPDATE_INFO_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse(fileName)
    }
  }

  @Test
  fun `test course updateInfo loaded`() {
    configureResponse("updateInfo.json")
    val updateInfo = MarketplaceConnector.getInstance().getMarketplaceCourseUpdateInfo(1)
    checkNotNull(updateInfo)
    assertEquals(102996, updateInfo.updateId)
    assertEquals(1, updateInfo.pluginId)
    assertEquals(3, updateInfo.version)
    assertEquals(13, updateInfo.compatibility.gte)
  }

  // course version from updateInfo.json should be incremented by 1
  @Test
  fun `test remote course version set from updateInfo`() {
    configureResponse("updateInfo.json")
    val course = EduCourse()
    course.setRemoteMarketplaceCourseVersion()
    assertEquals(4, course.marketplaceCourseVersion)
  }

  @Test
  fun `test remote course version is set when updateInfo is empty`() {
    configureResponse("emptyUpdateInfo.json")
    val course = EduCourse()
    course.setRemoteMarketplaceCourseVersion()
    assertEquals(1, course.marketplaceCourseVersion)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/updateInfo/"

  companion object {
    private val UPDATE_INFO_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}