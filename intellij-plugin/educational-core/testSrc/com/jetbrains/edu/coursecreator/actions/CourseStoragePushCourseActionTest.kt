package com.jetbrains.edu.coursecreator.actions

import com.intellij.util.application
import com.jetbrains.edu.coursecreator.actions.marketplace.courseStorage.CourseStoragePushCourse
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.CourseUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.rules.WithExperimentalFeature
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlin.test.Test

class CourseStoragePushCourseActionTest : EduActionTestCase() {
  @Test
  fun `test course storage push is not available in learner mode`() {
    mockJBUserInfo()
    createEduCourse(mode = CourseMode.STUDENT)
    testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test course storage push is not available for non-jb users`() {
    mockJBUserInfo(JBAccountUserInfo("test"))
    createEduCourse()
    testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test course storage push is not available for courses with marketplace id range`() {
    mockJBUserInfo()
    createEduCourse(id = 2)
    testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test course storage push is not available for non-marketplace courses`() {
    mockJBUserInfo()
    createEduCourse(isMarketplace = false)
    testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  @WithExperimentalFeature(EduExperimentalFeatures.COURSE_STORAGE, false)
  fun `test course storage push is not available without exp feature`() {
    mockJBUserInfo()
    createEduCourse()
    testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test course storage push uploads course when id is 0`() {
    mockJBUserInfo()
    val course = createEduCourse(id = 0)

    val mockConnector = mockService<CourseStorageConnector>(application)
    every { mockConnector.getLatestCourseUpdateInfo(any()) } returns null
    justRun { mockConnector.uploadNewCourse(project, course, any()) }

    val presentation = testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = true)

    assertEquals(presentation.text, EduCoreBundle.message("action.push.course.storage.upload.text"))
    verify(exactly = 1) { mockConnector.uploadNewCourse(project, course, any()) }
  }

  @Test
  fun `test course storage push updates course when id != 0`() {
    mockJBUserInfo()
    val course = createEduCourse()

    val mockConnector = mockService<CourseStorageConnector>(application)
    every { mockConnector.getLatestCourseUpdateInfo(any()) } returns CourseUpdateInfo(1, 1)
    justRun { mockConnector.uploadCourseUpdate(project, course, any()) }

    val presentation = testAction(CourseStoragePushCourse.ACTION_ID, shouldBeEnabled = true)

    assertEquals(presentation.text, EduCoreBundle.message("action.push.course.storage.update.text"))
    verify(exactly = 1) { mockConnector.uploadCourseUpdate(project, course, any()) }
  }

  private fun createEduCourse(
    mode: CourseMode = CourseMode.EDUCATOR,
    id: Int = 200_001,
    isMarketplace: Boolean = true,
  ): EduCourse {
    return courseWithFiles(courseMode = mode, id = id, createYamlConfigs = true) {}.apply {
      this.isMarketplace = isMarketplace
    } as EduCourse
  }

  private fun mockJBUserInfo(jbaUserInfo: JBAccountUserInfo = JBAccountUserInfo("test").apply { email = "test@jetbrains.com" }) {
    mockJBAccount(testRootDisposable)
    val mockSettings = mockService<MarketplaceSettings>(application)
    every { mockSettings.getJBAUserInfo() } returns jbaUserInfo
  }
}