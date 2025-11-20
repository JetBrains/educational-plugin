package com.jetbrains.edu.socialMedia

import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseMode.STUDENT
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInSettings
import com.jetbrains.edu.socialMedia.x.XPluginConfigurator
import com.jetbrains.edu.socialMedia.x.XSettings
import io.mockk.verify
import org.junit.Test
import java.nio.file.Path

class SocialMediaMultiplePostActionTest : SocialMediaPostActionTestBase() {

  override fun setUp() {
    super.setUp()
    ExtensionTestUtil.maskExtensions(XPluginConfigurator.EP_NAME, listOf(TestXConfigurator()), testRootDisposable)
    ExtensionTestUtil.maskExtensions(LinkedInPluginConfigurator.EP_NAME, listOf(TestLinkedInConfigurator()), testRootDisposable)
  }

  @Test
  fun `test post to social media`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task1")

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)
    assertFalse(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 1) { mockXConnector.tweet(X_MESSAGE, match { it.endsWith(X_GIF_PATH) }) }
    verify(exactly = 1) { mockLinkedInConnector.createPostWithMedia(any(), LINKEDIN_MESSAGE, match { it.endsWith(LINKEDIN_GIF_PATH) }) }
  }

  @Test
  fun `test respect social media settings 1`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task1")

    LinkedInSettings.getInstance().askToPost = false

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)
    assertFalse(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 1) { mockXConnector.tweet(X_MESSAGE, match { it.endsWith(X_GIF_PATH) }) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test respect social media settings 2`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task1")

    XSettings.getInstance().askToPost = false
    LinkedInSettings.getInstance().askToPost = false

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)
    assertTrue(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post for the second time`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task1")

    // Mark that the social media dialog was already shown
    SocialMediaPostManager.setAskedToPost(course.id)

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post for the local course`() {
    // given
    val course = createEduCourse(id = 0)
    val currentTask = course.findTask("lesson1", "task1")

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)
    assertFalse(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post for the educator course`() {
    // given
    val course = createEduCourse(courseMode = CourseMode.EDUCATOR)
    val currentTask = course.findTask("lesson1", "task1")

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)
    assertTrue(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post on failed task`() {
    // given
    val course = createEduCourse(solved = false)
    val currentTask = course.findTask("lesson1", "task1")

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)
    assertTrue(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post for the preview course`() {
    // given
    val course = createEduCourse()
    course.isPreview = true
    val currentTask = course.findTask("lesson1", "task1")

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)
    assertTrue(SocialMediaPostManager.needToAskedToPost(course.id))

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  private fun createEduCourse(
    id: Int = 123,
    courseMode: CourseMode = STUDENT,
    solved: Boolean = true
  ): EduCourse {
    return courseWithFiles(id = id, courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("taskFile1.txt")
          taskFile("checkResult.txt", if (solved) "Solved" else "Failed")
        }
      }
    } as EduCourse
  }
}

private const val X_MESSAGE = "My X message"
private const val X_GIF_PATH = "socialMedia/x/test_achievement.gif"
private const val LINKEDIN_MESSAGE = "My LinkedIn message"
private const val LINKEDIN_GIF_PATH = "socialMedia/linkedIn/test_achievement.gif"

private class TestLinkedInConfigurator : LinkedInPluginConfigurator {
  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean = true
  override fun getMessage(solvedTask: Task): String = LINKEDIN_MESSAGE

  override fun getIndexWithImagePath(
    solvedTask: Task,
    imageIndex: Int?
  ): Pair<Int?, Path?> {
    val finalImageIndex = imageIndex ?: 0
    val imagePath = SocialMediaUtils.pluginRelativePath(LINKEDIN_GIF_PATH)
    return finalImageIndex to imagePath
  }
}

private class TestXConfigurator : XPluginConfigurator {
  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean = true
  override fun getMessage(solvedTask: Task): String = X_MESSAGE

  override fun getIndexWithImagePath(
    solvedTask: Task,
    imageIndex: Int?
  ): Pair<Int?, Path?> {
    val finalImageIndex = imageIndex ?: 0
    val imagePath = SocialMediaUtils.pluginRelativePath(X_GIF_PATH)
    return finalImageIndex to imagePath
  }
}
