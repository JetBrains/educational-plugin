package com.jetbrains.edu.learning

import com.intellij.ui.EditorNotificationPanel
import org.jetbrains.annotations.TestOnly

class GeneratedRemoteInfoNotificationPanel : EditorNotificationPanel() {
  val text: String? = myLabel.text
}

class UnsolvedDependenciesNotificationPanel : EditorNotificationPanel() {
  @TestOnly
  fun getText(): String = myLabel.text
}