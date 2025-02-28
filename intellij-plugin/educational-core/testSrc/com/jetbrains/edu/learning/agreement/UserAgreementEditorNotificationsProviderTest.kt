package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.findFile
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedEditor
import com.jetbrains.edu.learning.submissions.UserAgreementState
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

    // Open task file in editor
    val virtualFile = project.courseDir.findFile("lesson/task/task.txt") ?: error("Failed to find virtual file for `task.txt`")
    myFixture.openFileInEditor(virtualFile)

    // Reset ignoring notification preference
    UserAgreementUtil.setEditorNotificationIgnored(ignored = false)
  }

  @Test
  fun `test editor notification is not shown when agreement is not shown`() {
    assertNull(getNotificationPanel())
  }

  @Test
  fun `test editor notification is not shown when agreement is accepted`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED
      )
    )

    assertNull(getNotificationPanel())
  }

  @Test
  fun `test editor notification is shown when agreement is declined`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED
      )
    )

    val virtualFile = project.selectedEditor?.virtualFile ?: error("Failed to retrieve virtual file in the editor")
    checkEditorNotification<UserAgreementEditorNotificationsProvider>(virtualFile, EduCoreBundle.message("user.agreement.editor.notification.text"))
  }

  @Test
  fun `test editor notification is not shown when user ignored it`() { // "Don't show again" is clicked
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED
      )
    )
    // and
    UserAgreementUtil.setEditorNotificationIgnored(ignored = true)

    assertNull(getNotificationPanel())
  }

  private fun getNotificationPanel(): EditorNotificationPanel? {
    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
    val notificationData = UserAgreementEditorNotificationsProvider().collectNotificationData(project, selectedEditor?.file!!)
    return notificationData?.apply(selectedEditor) as? EditorNotificationPanel
  }

  override fun tearDown() {
    try {
      UserAgreementUtil.setEditorNotificationIgnored(ignored = false)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}