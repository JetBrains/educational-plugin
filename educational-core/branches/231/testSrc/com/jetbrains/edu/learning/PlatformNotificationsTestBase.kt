package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationsImpl

// BACKCOMPAT: 2022.2. Merge with NotificationsTestBase
abstract class PlatformNotificationsTestBase : EduTestCase() {

  protected fun completeEditorNotificationAsyncTasks() {
    editorNotificationsImpl()?.completeAsyncTasks()
  }

  protected fun <T : EditorNotificationProvider> getNotificationPanels(
    fileEditor: FileEditor,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    return editorNotificationsImpl()?.getNotificationPanels(fileEditor)?.get(clazz) as? EditorNotificationPanel
  }

  private fun editorNotificationsImpl(): EditorNotificationsImpl? =
    EditorNotifications.getInstance(myFixture.project) as? EditorNotificationsImpl
}
