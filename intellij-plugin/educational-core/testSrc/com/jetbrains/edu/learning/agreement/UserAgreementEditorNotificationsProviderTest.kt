package com.jetbrains.edu.learning.agreement

import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.UserAgreementProperties
import com.jetbrains.edu.learning.agreement.UserAgreementUtil.setEditorNotificationIgnored
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.junit.Test

class UserAgreementEditorNotificationsProviderTest : NotificationsTestBase() {
  override fun setUp() {
    super.setUp()
    courseWithFiles(createYamlConfigs = true) {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", "some task text")
        }
      }
    }
  }

  @Test
  fun `test editor notification is not shown when agreement is not shown`() {
    checkNoEditorNotification<UserAgreementEditorNotificationsProvider>(findFile("lesson/task/task.txt"))
  }

  @Test
  fun `test editor notification is not shown when agreement is accepted`() {
    // when
    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    checkNoEditorNotification<UserAgreementEditorNotificationsProvider>(findFile("lesson/task/task.txt"))
  }

  @Test
  fun `test editor notification is shown when agreement is declined`() {
    // when
    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties.declined())

    checkEditorNotification<UserAgreementEditorNotificationsProvider>(
      findFile("lesson/task/task.txt"),
      EduCoreBundle.message("user.agreement.editor.notification.text")
    )
  }

  @Test
  fun `test editor notification is not shown when user ignored it`() { // "Don't show again" is clicked
    // when
    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties.declined())
    // and
    project.setEditorNotificationIgnored(ignored = true)

    checkNoEditorNotification<UserAgreementEditorNotificationsProvider>(findFile("lesson/task/task.txt"))
  }

  override fun tearDown() {
    try {
      project.setEditorNotificationIgnored(ignored = false)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}