package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationsImpl

abstract class PlatformNotificationsTestBase : EduTestCase() {

  protected fun <T : EditorNotifications.Provider<*>> getNotificationPanelForFileEditor(
    fileEditor: FileEditor,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    val provider = EditorNotificationsImpl.EP_PROJECT.findExtension(clazz, project) ?: error("`${clazz.canonicalName}` is not registered")
    return fileEditor.getUserData(provider.key) as? EditorNotificationPanel
  }
}
