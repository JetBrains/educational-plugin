package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.loadEncodedContent

/**
 * This is a copy of previous implementation of frameworkLessonUtils, which was updated in EDU-8362
 * It is planned to use the old implementation for CC framework lessons, before migrating to FileContents in EDU-7142
 *
 * TODO: remove this class and unify it with UserChanges in EDU-7142
 */
typealias FLTaskStateCC = Map<String, String>

/**
 * Returns [Change]s to convert [currentState] to [targetState]
 */
fun calculateChanges(
  currentState: FLTaskStateCC,
  targetState: FLTaskStateCC
): CCUserChanges {
  val changes = mutableListOf<CCChange>()
  val current = HashMap(currentState)
  loop@ for ((path, nextText) in targetState) {
    val currentText = current.remove(path)
    changes += when {
      currentText == null -> CCChange.AddFile(path, nextText)
      currentText != nextText -> CCChange.ChangeFile(path, nextText)
      else -> continue@loop
    }
  }

  current.mapTo(changes) { CCChange.RemoveFile(it.key) }
  return CCUserChanges(changes)
}

fun getTaskStateFromFiles(initialFiles: Set<String>, taskDir: VirtualFile): FLTaskStateCC {
  val documentManager = FileDocumentManager.getInstance()
  val currentState = HashMap<String, String>()
  for (path in initialFiles) {
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