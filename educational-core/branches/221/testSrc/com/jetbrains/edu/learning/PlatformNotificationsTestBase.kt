package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationsImpl

// BACKCOMPAT: 2021.3. Merge into it `NotificationsTestBase`
abstract class PlatformNotificationsTestBase : EduTestCase() {

  // BACKCOMPAT: 2021.3. Inline it
  protected fun <T : EditorNotificationProvider> getNotificationPanelForFileEditor(
    fileEditor: FileEditor,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    return EditorNotificationsImpl.getNotificationPanels(fileEditor)?.get(clazz) as? EditorNotificationPanel
  }
}
