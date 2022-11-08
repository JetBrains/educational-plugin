package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationsImpl

abstract class PlatformNotificationsTestBase : EduTestCase() {

  protected fun completeEditorNotificationAsyncTasks() {
    EditorNotificationsImpl.completeAsyncTasks()
  }

  protected fun <T : EditorNotificationProvider> getNotificationPanels(
    fileEditor: FileEditor,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    return EditorNotificationsImpl.getNotificationPanels(fileEditor)[clazz] as? EditorNotificationPanel
  }
}
