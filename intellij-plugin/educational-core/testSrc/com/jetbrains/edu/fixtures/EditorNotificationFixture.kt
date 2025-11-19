package com.jetbrains.edu.fixtures

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationsImpl
import org.junit.Assert

class EditorNotificationFixture(private val projectProvider: () -> Project) {

  inline fun <reified T : EditorNotificationProvider> checkEditorNotification(
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
    EditorNotifications.getInstance(projectProvider()) as? EditorNotificationsImpl

  inline fun <reified T : EditorNotificationProvider> checkNoEditorNotification(virtualFile: VirtualFile) {
    checkNoEditorNotification(virtualFile, T::class.java)
  }

  fun <T : EditorNotificationProvider> checkEditorNotification(
    virtualFile: VirtualFile,
    clazz: Class<T>,
    expectedMessage: String? = null
  ) {
    val notificationPanel = getNotificationPanel(virtualFile, clazz)
    Assert.assertNotNull("Notification not shown", notificationPanel)
    if (expectedMessage != null) {
      Assert.assertEquals("Panel text is incorrect", expectedMessage, notificationPanel?.text)
    }
  }

  fun <T : EditorNotificationProvider> checkNoEditorNotification(virtualFile: VirtualFile, clazz: Class<T>) {
    val notificationPanel = getNotificationPanel(virtualFile, clazz)
    Assert.assertNull("Notification is shown", notificationPanel)
  }

  private fun <T : EditorNotificationProvider> getNotificationPanel(
    virtualFile: VirtualFile,
    clazz: Class<T>
  ): EditorNotificationPanel? {
    val project = projectProvider()
    FileEditorManager.getInstance(project).openFile(virtualFile, true)
    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
                     ?: error("Can't find file editor for $virtualFile")

    return getNotificationPanels(fileEditor, clazz)
  }
}
