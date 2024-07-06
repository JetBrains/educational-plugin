package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.COMPLETED
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class EduTestInfoTest(
  private val eduTestInfo: EduTestInfo,
  @Suppress("UNUSED_PARAMETER") testSuffix: String
) : EduTestCase() {
  private val objectMapper by lazy { MarketplaceSubmissionsConnector.getInstance().objectMapper }

  @Test
  fun `test EduTestInfo serialization`() {
    val serialized = objectMapper.writeValueAsString(eduTestInfo)
    assertEquals(eduTestInfo.expectedJson(), serialized)
  }

  private fun EduTestInfo.expectedJson(): String = """{"name":"$name","status":$status}"""

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{1}")
    fun testInfos(): Collection<Array<Any>> = listOf(
      arrayOf(EduTestInfo("testName", COMPLETED), "PresentableStatus"),
      arrayOf(EduTestInfo("testName", 2, ""), "status"),
      arrayOf(EduTestInfo("testName", 3, "testMessage"), "message"),
      arrayOf(EduTestInfo("testName", 4, "testMessage", details = "testDetails"), "details"),
      arrayOf(EduTestInfo("testName", 5, "testMessage", isFinishedSuccessfully = false), "isFinishedSuccessfully"),
      arrayOf(EduTestInfo("testName", 6, "testMessage", checkResultDiff = CheckResultDiff("expected", "actual")), "checkResultDiff"),
    )
  }
}