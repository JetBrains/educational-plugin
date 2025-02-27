package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import org.junit.Test

class UserAgreementEditorNotificationsProviderTest : EduTestCase() {
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
    val task = getCourse().findTask("lesson", "task")
    val taskFile = task.getTaskFile("task.txt") ?: error("Can't find task file for ${task.name}")
    val virtualFile = taskFile.getVirtualFile(project) ?: error("Can't find virtual file for `${taskFile.name}` task")
    myFixture.openFileInEditor(virtualFile)
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

    assertEquals(EduCoreBundle.message("user.agreement.editor.notification.text"), getNotificationPanel()?.text)
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
    UserAgreementUtil.setIgnoreNotification()

    assertNull(getNotificationPanel())
  }

  private fun getNotificationPanel(): EditorNotificationPanel? {
    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
    val notificationData = UserAgreementEditorNotificationsProvider().collectNotificationData(project, selectedEditor?.file!!)
    return notificationData?.apply(selectedEditor) as? EditorNotificationPanel
  }
}