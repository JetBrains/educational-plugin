package com.jetbrains.edu.socialMedia

import com.intellij.util.application
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInAccount
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInConnector
import com.jetbrains.edu.socialMedia.linkedIn.create
import com.jetbrains.edu.socialMedia.suggestToPostDialog.SuggestToPostDialogUI
import com.jetbrains.edu.socialMedia.suggestToPostDialog.withMockSuggestToPostDialogUI
import com.jetbrains.edu.socialMedia.x.XAccount
import com.jetbrains.edu.socialMedia.x.XConnector
import com.jetbrains.edu.socialMedia.x.api.TweetData
import com.jetbrains.edu.socialMedia.x.api.TweetResponse
import com.jetbrains.edu.socialMedia.x.create
import io.mockk.every

abstract class SocialMediaPostActionTestBase : EduActionTestCase() {

  protected lateinit var mockXConnector: XConnector
  protected lateinit var mockLinkedInConnector: LinkedInConnector

  override fun setUp() {
    super.setUp()

    mockXConnector = mockService<XConnector>(application)
    mockXConnector.account = XAccount.Factory.create()
    every { mockXConnector.tweet(any(), any()) } answers { TweetResponse(TweetData("123", firstArg())) }

    mockLinkedInConnector = mockService<LinkedInConnector>(application)
    mockLinkedInConnector.account = LinkedInAccount.Factory.create()
    every { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) } returns Unit
  }

  protected fun launchCheckAction(task: Task): Boolean {
    val currentTask = task.lesson.asSafely<FrameworkLesson>()?.currentTask()
    NavigationUtils.navigateToTask(project, task, currentTask)

    var isDialogShown = false
    withMockSuggestToPostDialogUI(object : SuggestToPostDialogUI {
      override fun showAndGet(): Boolean {
        isDialogShown = true
        return true
      }
    }) {
      testAction(CheckAction(task.getUICheckLabel()))
    }

    return isDialogShown
  }
}
