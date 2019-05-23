package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.ui.EditorNotificationsImpl
import com.jetbrains.edu.learning.EduTestCase

abstract class PlaceholderDependencyTestBase : EduTestCase() {

  protected fun completeEditorNotificationAsyncTasks() {
    EditorNotificationsImpl.completeAsyncTasks()
  }
}
