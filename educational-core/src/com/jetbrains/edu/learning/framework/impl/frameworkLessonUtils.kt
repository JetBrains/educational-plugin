package com.jetbrains.edu.learning.framework.impl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.diff.SimpleConflictResolveStrategy
import com.jetbrains.edu.learning.framework.impl.diff.FLConflictResolveStrategy
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.loadEncodedContent
import com.jetbrains.edu.learning.messages.EduCoreBundle

internal typealias State = Map<String, String>

internal fun applyChanges(changes: UserChanges, initialState: State = emptyMap()): State {
  return HashMap(initialState).apply { changes.apply(this) }
}

fun State.complement(intersection: Set<Map.Entry<String, String>>): State = entries.subtract(intersection).associate { it.key to it.value }

fun State.complementByKeys(intersection: Set<Map.Entry<String, String>>): State {
  val keysIntersection = intersection.map { it.key }.toSet()
  return filter { it.key !in keysIntersection }
}

/**
 * Returns [Change]s to convert [currentState] to [targetState]
 */
internal fun calculateChanges(
  currentState: State,
  targetState: State
): UserChanges {
  val changes = mutableListOf<Change>()
  val current = HashMap(currentState)
  loop@ for ((path, nextText) in targetState) {
    val currentText = current.remove(path)
    changes += when {
      currentText == null -> Change.AddFile(path, nextText)
      currentText != nextText -> Change.ChangeFile(path, nextText)
      else -> continue@loop
    }
  }

  current.mapTo(changes) { Change.RemoveFile(it.key) }
  return UserChanges(changes)
}

internal fun State.split(task: Task): Pair<State, State> {
  val visibleFiles = HashMap<String, String>()
  val invisibleFiles = HashMap<String, String>()

  for ((path, text) in this) {
    // TaskFiles and state may not be consistent due to external changes in hyperskill lessons.
    // if there is a task in state that is not in taskFiles, then we know that it is a visible file.
    val isVisibleFile = task.taskFiles[path]?.isVisible ?: true
    val state = if (isVisibleFile) visibleFiles else invisibleFiles
    state[path] = text
  }

  return visibleFiles to invisibleFiles
}

internal val Task.allFiles: State get() = taskFiles.mapValues { it.value.text }

// we propagate only visible files
internal val Task.allPropagatableFiles: State
  get() = taskFiles.filterValues { it.isVisible }.mapValues { it.value.text }

internal fun chooseConflictResolveStrategy(): FLConflictResolveStrategy {
  return SimpleConflictResolveStrategy()
}

internal fun getVFSTaskState(initialFiles: State, taskDir: VirtualFile): State {
  val documentManager = FileDocumentManager.getInstance()
  val currentState = HashMap<String, String>()
  for ((path, _) in initialFiles) {
    val file = taskDir.findFileByRelativePath(path) ?: continue

    val text = if (file.isToEncodeContent) {
      file.loadEncodedContent(isToEncodeContent = true)
    }
    else {
      runReadAction { documentManager.getDocument(file)?.text }
    }

    if (text == null) {
      continue
    }

    currentState[path] = text
  }
  return currentState
}

internal fun showApplyChangesCanceledNotification(project: Project, startTaskName: String, cancelledTaskName: String) {
  val notification = Notification(
    "JetBrains Academy",
    EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.cancel.title"),
    EduCoreBundle.message(
      "action.Educational.Educator.ApplyChangesToNextTasks.Notification.cancel.description",
      startTaskName,
      cancelledTaskName
    ),
    NotificationType.WARNING
  )
  Notifications.Bus.notify(notification, project)
}

internal fun showApplyChangesSuccessNotification(project: Project, startTaskName: String) {
  val notification = Notification(
    "JetBrains Academy",
    EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.success.title"),
    EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.success.description", startTaskName),
    NotificationType.INFORMATION
  )
  Notifications.Bus.notify(notification, project)
}