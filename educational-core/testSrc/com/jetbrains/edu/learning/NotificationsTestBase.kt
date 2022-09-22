package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationsImpl

abstract class NotificationsTestBase : EduTestCase() {

  private fun completeEditorNotificationAsyncTasks() {
    EditorNotificationsImpl.completeAsyncTasks()
  }

  protected inline fun <reified T : EditorNotifications.Provider<*>> checkEditorNotification(
    virtualFile: VirtualFile,
    expectedMessage: String? = null
  ) {
    checkEditorNotification(virtualFile, T::class.java, expectedMessage)
  }

  protected inline fun <reified T : EditorNotifications.Provider<*>> checkNoEditorNotification(virtualFile: VirtualFile) {
    checkNoEditorNotification(virtualFile, T::class.java)
  }

  protected fun <T : EditorNotifications.Provider<*>> checkEditorNotification(
    virtualFile: VirtualFile,
    clazz: Class<T>,
    expectedMessage: String? = null
  ) {
    val notificationPanel = getNotificationPanel(virtualFile, clazz)
    assertNotNull("Notification not shown", notificationPanel)
    if (expectedMessage != null) {
      assertEquals("Panel text is incorrect", expectedMessage, notificationPanel?.text)
    }
  }

  protected fun <T : EditorNotifications.Provider<*>> checkNoEditorNotification(virtualFile: VirtualFile, clazz: Class<T>) {
    val notificationPanel = getNotificationPanel(virtualFile, clazz)
    assertNull("Notification is shown", notificationPanel)
  }

  private fun <T : EditorNotifications.Provider<*>> getNotificationPanel(
    virtualFile: VirtualFile,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    myFixture.openFileInEditor(virtualFile)
    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
                     ?: error("Can't find file editor for $virtualFile")

    return EditorNotificationsImpl.getNotificationPanels(fileEditor)[clazz] as? EditorNotificationPanel
  }

  protected fun withYamlFileTypeRegistered(action: () -> Unit) {
    try {
      runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
      action()
    } finally {
      runWriteAction { FileTypeManager.getInstance().removeAssociatedExtension(PlainTextFileType.INSTANCE, "yaml") }
    }
  }
}
