package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationsImpl

abstract class NotificationsTestBase : EduTestCase() {

  protected inline fun <reified T : EditorNotificationProvider> checkEditorNotification(
    virtualFile: VirtualFile,
    expectedMessage: String? = null
  ) {
    checkEditorNotification(virtualFile, T::class.java, expectedMessage)
  }

  private fun completeEditorNotificationAsyncTasks() {
    editorNotificationsImpl()?.completeAsyncTasks()
  }

  private fun <T : EditorNotificationProvider> getNotificationPanels(
    fileEditor: FileEditor,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    return editorNotificationsImpl()?.getNotificationPanels(fileEditor)?.get(clazz as Class<*>) as? EditorNotificationPanel
  }

  private fun editorNotificationsImpl(): EditorNotificationsImpl? =
    EditorNotifications.getInstance(myFixture.project) as? EditorNotificationsImpl

  protected inline fun <reified T : EditorNotificationProvider> checkNoEditorNotification(virtualFile: VirtualFile) {
    checkNoEditorNotification(virtualFile, T::class.java)
  }

  protected fun <T : EditorNotificationProvider> checkEditorNotification(
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

  protected fun <T : EditorNotificationProvider> checkNoEditorNotification(virtualFile: VirtualFile, clazz: Class<T>) {
    val notificationPanel = getNotificationPanel(virtualFile, clazz)
    assertNull("Notification is shown", notificationPanel)
  }

  private fun <T : EditorNotificationProvider> getNotificationPanel(
    virtualFile: VirtualFile,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    myFixture.openFileInEditor(virtualFile)
    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
                     ?: error("Can't find file editor for $virtualFile")

    return getNotificationPanels(fileEditor, clazz)
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
