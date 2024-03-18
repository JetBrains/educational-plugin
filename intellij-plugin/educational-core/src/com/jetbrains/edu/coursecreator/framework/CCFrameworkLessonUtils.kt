package com.jetbrains.edu.coursecreator.framework

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.courseFormat.SyncChangesTaskFileState
import com.jetbrains.edu.learning.courseFormat.TaskFile
import javax.swing.Icon

fun getSyncChangesIcon(taskFile: TaskFile): Icon? = when (taskFile.syncChangesIcon) {
  SyncChangesTaskFileState.NONE -> null
  SyncChangesTaskFileState.INFO -> AllIcons.General.Information
  SyncChangesTaskFileState.WARNING -> AllIcons.General.Warning
}
