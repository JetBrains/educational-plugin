package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.fixtures.EditorNotificationFixture

abstract class NotificationsTestBase : EduTestCase() {

  protected val notificationFixture: EditorNotificationFixture = EditorNotificationFixture { project }

  protected inline fun <reified T : EditorNotificationProvider> checkEditorNotification(
    virtualFile: VirtualFile,
    expectedMessage: String? = null
  ) {
    notificationFixture.checkEditorNotification<T>(virtualFile, expectedMessage)
  }

  protected inline fun <reified T : EditorNotificationProvider> checkNoEditorNotification(virtualFile: VirtualFile) {
    notificationFixture.checkNoEditorNotification<T>(virtualFile)
  }
}
