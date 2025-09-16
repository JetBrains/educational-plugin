package com.jetbrains.edu.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.BinaryContentsFromDisk
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.TaskFile

typealias FLTaskState = Map<String, FileContents>

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
      !currentText.textRepresentationEquals(nextText) -> Change.ChangeFile(path, nextText)
      else -> continue@loop
    }
  }

  current.mapTo(changes) { Change.RemoveFile(it.key) }
  return UserChanges(changes)
}

fun getTaskStateFromFiles(initialFiles: Iterable<TaskFile>, taskDir: VirtualFile): FLTaskState {
  val currentState = HashMap<String, FileContents>()
  for (taskFile in initialFiles) {
    val file = taskDir.findFileByRelativePath(taskFile.name) ?: continue

    val diskContents = when (taskFile.contents) {
      is BinaryContents -> BinaryContentsFromDisk(file)
      else -> {
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) } ?: continue
        InMemoryTextualContents(document.text)
      }
    }
    currentState[taskFile.name] = diskContents
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

fun FLTaskState.stateEquals(other: FLTaskState): Boolean {
  if (keys != other.keys) return false
  return all { (path, contents1) ->
    val contents2 = other[path] ?: return false
    contents1.textRepresentationEquals(contents2)
  }
}


/**
 * Simple equality check for [FileContents].
 *
 * TODO: Implement a proper equality check that uses [BinaryContents.bytes] for comparing binary contents
 *
 * @return true if [other] has the same [FileContents.textualRepresentation]
 */
fun FileContents.textRepresentationEquals(other: FileContents?): Boolean = textualRepresentation == other?.textualRepresentation
