package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationsImpl

abstract class NotificationsTestBase : EduTestCase() {

  private fun completeEditorNotificationAsyncTasks() {
    EditorNotificationsImpl.completeAsyncTasks()
  }

  protected fun checkEditorNotification(
    virtualFile: VirtualFile,
    notificationKey: Key<EditorNotificationPanel>,
    expectedMessage: String? = null
  ) {
    myFixture.openFileInEditor(virtualFile)
    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
    val notificationPanel = fileEditor?.getUserData(notificationKey)
    assertNotNull("Notification not shown", notificationPanel)
    if (expectedMessage != null) {
      assertEquals("Panel text is incorrect", expectedMessage, notificationPanel?.text)
    }
  }

  protected fun checkNoEditorNotification(virtualFile: VirtualFile, notificationKey: Key<EditorNotificationPanel>) {
    myFixture.openFileInEditor(virtualFile)
    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)!!
    val notificationPanel = fileEditor.getUserData(notificationKey)
    assertNull("Notification is shown", notificationPanel)
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
