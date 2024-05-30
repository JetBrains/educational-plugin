package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.impl.event.DocumentEventImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

/**
 * Listens changes in study files and updates
 * coordinates of all the placeholders in current task file
 */
class EduDocumentListener private constructor(
  holder: CourseInfoHolder<out Course?>,
  /**
   * If [taskFile] is `null` than listener should determine affected task file by [DocumentEvent],
   * otherwise, it should track changes only in single [Document] related to [taskFile]
   */
  private val taskFile: TaskFile?
) : EduDocumentListenerBase(holder) {

  private val updateYaml: Boolean = taskFile == null

  override fun beforeDocumentChange(e: DocumentEvent) {
    if (taskFile == null && !e.isInProjectContent()) return
    val taskFile = (taskFile ?: e.taskFile) ?: return
    if (!taskFile.isTrackChanges) {
      return
    }
    if (taskFile.errorHighlightLevel == EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      taskFile.errorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS

      val project = holder.course?.project
      if (project != null) {
        taskFile.getVirtualFile(project)?.setHighlightLevel(project, taskFile.errorHighlightLevel)
      }
    }
  }

  override fun documentChanged(e: DocumentEvent) {
    if (taskFile == null && !e.isInProjectContent()) return
    val taskFile = (taskFile ?: e.taskFile) ?: return
    if (!taskFile.isTrackChanges) {
      return
    }

    val project = holder.course?.project
    if (project != null) {
      SyncChangesStateManager.getInstance(project).taskFileChanged(taskFile)
    }

    if (taskFile.answerPlaceholders.isEmpty()) return

    if (e !is DocumentEventImpl) {
      return
    }

    val offset = e.getOffset()
    val change = e.getNewLength() - e.getOldLength()

    val fragment = e.getNewFragment()
    val oldFragment = e.getOldFragment()

    for (placeholder in taskFile.answerPlaceholders) {
      var placeholderStart = placeholder.offset
      var placeholderEnd = placeholder.endOffset

      val changes = getChangeForOffsets(offset, change, placeholder)
      val changeForStartOffset = changes.getFirst()
      val changeForEndOffset = changes.getSecond()

      placeholderStart += changeForStartOffset
      placeholderEnd += changeForEndOffset

      if (placeholderStart - 1 == offset && fragment.toString().isEmpty() && oldFragment.toString().startsWith("\n")) {
        placeholderStart -= 1
      }

      if (placeholderStart == offset && oldFragment.toString().isEmpty() && fragment.toString().startsWith("\n")) {
        placeholderStart += 1
      }

      val length = placeholderEnd - placeholderStart
      assert(length >= 0)
      assert(placeholderStart >= 0)
      updatePlaceholder(placeholder, placeholderStart, length)
    }
  }

  private fun getChangeForOffsets(offset: Int, change: Int, placeholder: AnswerPlaceholder): Pair<Int, Int> {
    val placeholderStart = placeholder.offset
    val placeholderEnd = placeholder.endOffset
    var start = 0
    var end = change
    if (offset > placeholderEnd) {
      return Pair.create(0, 0)
    }

    if (offset < placeholderStart) {
      start = change

      if (change < 0 && offset - change > placeholderStart) {  // delete part of placeholder start
        start = offset - placeholderStart
      }
    }

    if (change < 0 && offset - change > placeholderEnd) {   // delete part of placeholder end
      end = offset - placeholderEnd
    }

    return Pair.create(start, end)
  }

  private fun updatePlaceholder(answerPlaceholder: AnswerPlaceholder, start: Int, length: Int) {
    answerPlaceholder.offset = start
    answerPlaceholder.length = length
    if (updateYaml) {
      YamlFormatSynchronizer.saveItem(answerPlaceholder.taskFile.task)
    }
  }

  private val DocumentEvent.taskFile: TaskFile? get() {
    val file = FileDocumentManager.getInstance().getFile(document) ?: return null
    return file.getTaskFile(holder)
  }

  companion object {
    fun setGlobalListener(project: Project, disposable: Disposable) {
      EditorFactory.getInstance().eventMulticaster.addDocumentListener(EduDocumentListener(project.toCourseInfoHolder(), null), disposable)
    }

    /**
     * Should be used only when current course doesn't contain task file related to given [file].
     * For example, when changes are performed on non-physical file.
     */
    fun runWithListener(project: Project, taskFile: TaskFile, file: VirtualFile, action: (Document) -> Unit) {
      return runWithListener(project.toCourseInfoHolder(), taskFile, file, action)
    }

    /**
     * Should be used only when current course doesn't contain task file related to given [file].
     * For example, when changes are performed on non-physical file.
     */
    fun runWithListener(holder: CourseInfoHolder<out Course?>, taskFile: TaskFile, file: VirtualFile, action: (Document) -> Unit) {
      require(file.getTaskFile(holder) == null) {
        "Changes in `${taskFile.name}` should be tracked by global listener"
      }
      val document = FileDocumentManager.getInstance().getDocument(file) ?: return

      val listener = EduDocumentListener(holder, taskFile)
      document.addDocumentListener(listener)
      try {
        action(document)
      }
      finally {
        document.removeDocumentListener(listener)
      }
    }

    fun modifyWithoutListener(task: Task, pathInTask: String, modification: () -> Unit) {
      val taskFile = task.getTaskFile(pathInTask) ?: return
      val isTrackChanges = taskFile.isTrackChanges
      taskFile.isTrackChanges = false
      try {
        modification()
      } finally {
        taskFile.isTrackChanges = isTrackChanges
      }
    }
  }
}
