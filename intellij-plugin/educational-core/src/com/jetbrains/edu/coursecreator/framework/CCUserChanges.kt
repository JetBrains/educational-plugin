package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.doWithoutReadOnlyAttribute
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.removeWithEmptyParents
import com.jetbrains.edu.learning.toCourseInfoHolder
import org.apache.commons.codec.binary.Base64
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Used to store changes made by CC in framework lessons for `Sync Changes` feature.
 *
 * This is a copy of previous implementation of UserChanges, which was updated in EDU-8362
 * It is planned to use the old implementation for CC framework lessons, before migrating to FileContents in EDU-7142
 *
 * TODO: remove this class and unify it with UserChanges in EDU-7142
 */
class CCUserChanges(val changes: List<CCChange>, val timestamp: Long = System.currentTimeMillis()) : FrameworkStorageData {

  operator fun plus(otherChanges: List<CCChange>): CCUserChanges = CCUserChanges(changes + otherChanges)

  fun apply(project: Project, taskDir: VirtualFile, task: Task) {
    for (change in changes) {
      change.apply(project, taskDir, task)
    }
  }

  fun apply(state: MutableMap<String, String>) {
    for (change in changes) {
      change.apply(state)
    }
  }

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { CCChange.writeChange(it, out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {

    private val EMPTY = CCUserChanges(emptyList(), -1)

    fun empty(): CCUserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): CCUserChanges {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<CCChange>(size)
      for (i in 0 until size) {
        changes += CCChange.readChange(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      return CCUserChanges(changes, timestamp)
    }
  }
}

sealed class CCChange {
  val path: String
  val text: String

  constructor(path: String, text: String) {
    this.path = path
    this.text = text
  }

  @Throws(IOException::class)
  constructor(input: DataInput) {
    this.path = input.readUTF()
    this.text = input.readUTF()
  }

  @Throws(IOException::class)
  protected fun write(out: DataOutput) {
    out.writeUTF(path)
    out.writeUTF(text)
  }

  abstract fun apply(project: Project, taskDir: VirtualFile, task: Task)
  abstract fun apply(state: MutableMap<String, String>)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CCChange) return false
    if (javaClass != other.javaClass) return false

    if (path != other.path) return false
    if (text != other.text) return false

    return true
  }

  override fun hashCode(): Int {
    var result = path.hashCode()
    result = 31 * result + text.hashCode()
    return result
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(path='$path', text='$text')"
  }

  class AddFile : CCChange {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      if (task.getTaskFile(path) == null) {
        GeneratorUtils.createChildFile(project, taskDir, path, text)
      }
      else {
        try {
          EduDocumentListener.modifyWithoutListener(task, path) {
            GeneratorUtils.createChildFile(project, taskDir, path, text)
          }
        }
        catch (e: IOException) {
          LOG.error("Failed to create file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveFile : CCChange {

    constructor(path: String) : super(path, "")

    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      runUndoTransparentWriteAction {
        try {
          taskDir.findFileByRelativePath(path)?.removeWithEmptyParents(taskDir)
        }
        catch (e: IOException) {
          LOG.error("Failed to delete file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state -= path
    }
  }

  class ChangeFile : CCChange {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val file = taskDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("Can't find file `$path` in `$taskDir`")
        return
      }

      if (file.isToEncodeContent) {
        file.doWithoutReadOnlyAttribute {
          runWriteAction {
            file.setBinaryContent(Base64.decodeBase64(text))
          }
        }
      }
      else {
        EduDocumentListener.modifyWithoutListener(task, path) {
          val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
          if (document != null) {
            val expandedText = StringUtil.convertLineSeparators(EduMacroUtils.expandMacrosForFile(project.toCourseInfoHolder(), file, text))
            file.doWithoutReadOnlyAttribute {
              runUndoTransparentWriteAction { document.setText(expandedText) }
            }
          }
          else {
            LOG.warn("Can't get document for `$file`")
          }
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class PropagateLearnerCreatedTaskFile : CCChange {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val taskFile = TaskFile(path, text).apply { isLearnerCreated = true }
      task.addTaskFile(taskFile)
    }


    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveTaskFile : CCChange {

    constructor(path: String): super(path, "")
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      task.removeTaskFile(path)
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCChange::class.java)

    @Throws(IOException::class)
    fun writeChange(change: CCChange, out: DataOutput) {
      val ordinal = when (change) {
        is AddFile -> 0
        is RemoveFile -> 1
        is ChangeFile -> 2
        is PropagateLearnerCreatedTaskFile -> 3
        is RemoveTaskFile -> 4
      }
      out.writeInt(ordinal)
      change.write(out)
    }

    @Throws(IOException::class)
    fun readChange(input: DataInput): CCChange {
      val ordinal = input.readInt()
      return when (ordinal) {
        0 -> AddFile(input)
        1 -> RemoveFile(input)
        2 -> ChangeFile(input)
        3 -> PropagateLearnerCreatedTaskFile(input)
        4 -> RemoveTaskFile(input)
        else -> error("Unexpected change type: $ordinal")
      }
    }
  }
}
