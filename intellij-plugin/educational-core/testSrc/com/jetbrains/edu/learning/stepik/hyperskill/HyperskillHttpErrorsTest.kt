package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection.*

class HyperskillHttpErrorsTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  @Test
  fun `test service is under maintenance`() = doTest(HTTP_BAD_GATEWAY, EduCoreBundle.message("error.network.service.maintenance"))
  @Test
  fun `test service is down`() = doTest(HTTP_GATEWAY_TIMEOUT, EduCoreBundle.message("error.network.service.down"))
  @Test
  fun `test unexpected error occurred`() = doTest(HTTP_BAD_REQUEST, EduCoreBundle.message("error.network.unexpected.error", ""))
  @Test
  fun `test forbidden`() = doTest(HTTP_FORBIDDEN, EduCoreBundle.message("error.network.access.denied"))

  private fun doTest(code: Int, expectedError: String) {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ -> MockResponse().setResponseCode(code) }
    val response = mockConnector.postSubmission(StepikBasedSubmission())
    val actualError = (response as Err).error
    assertTrue("Unexpected error message: `$actualError`", actualError.startsWith(expectedError))
  }
}
