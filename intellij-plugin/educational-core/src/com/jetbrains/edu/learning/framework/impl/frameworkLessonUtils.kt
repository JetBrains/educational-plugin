package com.jetbrains.edu.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.loadEncodedContent

typealias FLTaskState = Map<String, String>

/**
 * Returns [Change]s to convert [currentState] to [targetState]
 */
fun calculateChanges(
  currentState: FLTaskState,
  targetState: FLTaskState
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

fun getTaskStateFromFiles(initialFiles: Set<String>, taskDir: VirtualFile): FLTaskState {
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

fun LessonContainer.visitFrameworkLessons(visit : (FrameworkLesson) -> Unit) {
  visitLessons {
    if (it is FrameworkLesson) {
      visit(it)
    }
  }
}