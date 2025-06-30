package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.marketplace.lti.LtiCourseParamsProcessor.Companion.LTI_LAUNCH_ID
import com.jetbrains.edu.learning.marketplace.lti.LtiCourseParamsProcessor.Companion.LTI_LMS_DESCRIPTION
import com.jetbrains.edu.learning.marketplace.lti.LtiCourseParamsProcessor.Companion.LTI_STUDY_ITEM_ID
import com.jetbrains.edu.learning.navigation.NavigationProperties
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseParamsProcessorTestBase
import org.junit.Test

class LtiCourseParamsProcessorTest : CourseParamsProcessorTestBase() {

  @Test
  fun `lti metadata`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "description"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettings("id", "description", LTIOnlineService.STANDALONE),
      LTISettingsManager.getInstance(project).state
    )
    assertEquals(NavigationProperties(-1), StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Test
  fun `lti metadata with study item id`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "description",
      LTI_STUDY_ITEM_ID to "12345"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettings("id", "description", LTIOnlineService.STANDALONE),
      LTISettingsManager.getInstance(project).state
    )
    assertEquals(NavigationProperties(12345), StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Test
  fun `missing required parameters`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_STUDY_ITEM_ID to "12345"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettings(null, null, LTIOnlineService.ALPHA_TEST_2024),
      LTISettingsManager.getInstance(project).state
    )
    assertNull(StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Suppress("TestFunctionName")
  private fun LTISettings(launchId: String?, lmsDescription: String?, onlineService: LTIOnlineService): LTISettings {
    return LTISettings().apply {
      this.launchId = launchId
      this.lmsDescription = lmsDescription
      this.onlineService = onlineService
    }
  }
}
