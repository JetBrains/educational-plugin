package com.jetbrains.edu.learning.framework.impl

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
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.doWithoutReadOnlyAttribute
import com.jetbrains.edu.learning.removeWithEmptyParents
import com.jetbrains.edu.learning.toCourseInfoHolder
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class UserChanges(val changes: List<Change>, val timestamp: Long = System.currentTimeMillis()) : FrameworkStorageData {

  operator fun plus(otherChanges: List<Change>): UserChanges = UserChanges(changes + otherChanges)

  fun apply(project: Project, taskDir: VirtualFile, task: Task) {
    for (change in changes) {
      change.apply(project, taskDir, task)
    }
  }

  fun apply(state: MutableMap<String, FileContents>) {
    for (change in changes) {
      change.apply(state)
    }
  }

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {
    private val EMPTY = UserChanges(emptyList(), -1)

    fun empty(): UserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      return UserChanges(changes, timestamp)
    }
  }
}

sealed class Change {
  val path: String
  val contents: FileContents

  constructor(path: String, contents: FileContents) {
    this.path = path
    this.contents = contents
  }

  @Throws(IOException::class)
  constructor(input: DataInput) {
    this.path = input.readUTF()
    this.contents = UserChangesContents.read(input)
  }

  @Throws(IOException::class)
  protected fun write(out: DataOutput) {
    out.writeUTF(path)
    UserChangesContents.write(contents, out)
  }

  abstract fun apply(project: Project, taskDir: VirtualFile, task: Task)
  abstract fun apply(state: MutableMap<String, FileContents>)

  override fun toString(): String {
    return "${javaClass.simpleName}(path='$path', contents='$contents')"
  }

  class AddFile : Change {
    constructor(path: String, contents: FileContents): super(path, contents)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      if (task.getTaskFile(path) == null) {
        GeneratorUtils.createChildFile(project, taskDir, path, contents)
      }
      else {
        try {
          EduDocumentListener.modifyWithoutListener(task, path) {
            GeneratorUtils.createChildFile(project, taskDir, path, contents)
          }
        }
        catch (e: IOException) {
          LOG.error("Failed to create file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, FileContents>) {
      state[path] = contents
    }
  }

  class RemoveFile : Change {

    constructor(path: String) : super(path, UndeterminedContents.EMPTY)

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

    override fun apply(state: MutableMap<String, FileContents>) {
      state -= path
    }
  }

  class ChangeFile : Change {

    constructor(path: String, contents: FileContents): super(path, contents)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val file = taskDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("Can't find file `$path` in `$taskDir`")
        return
      }

      when (contents) {
        is BinaryContents -> {
          file.doWithoutReadOnlyAttribute {
            runWriteAction {
              file.setBinaryContent(contents.bytes)
            }
          }
        }
        else -> {
          EduDocumentListener.modifyWithoutListener(task, path) {
            val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
            if (document != null) {
              val expandedText = EduMacroUtils.expandMacrosForFile(project.toCourseInfoHolder(), file, contents.textualRepresentation)
              val newText = StringUtil.convertLineSeparators(expandedText)
              file.doWithoutReadOnlyAttribute {
                runUndoTransparentWriteAction { document.setText(newText) }
              }
            }
            else {
              LOG.warn("Can't get document for `$file`")
            }
          }
        }
      }
    }

    override fun apply(state: MutableMap<String, FileContents>) {
      state[path] = contents
    }
  }

  class PropagateLearnerCreatedTaskFile : Change {
    constructor(path: String, contents: FileContents): super(path, contents)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val taskFile = TaskFile(path, contents).apply { isLearnerCreated = true }
      task.addTaskFile(taskFile)
    }


    override fun apply(state: MutableMap<String, FileContents>) {
      state[path] = contents
    }
  }

  class RemoveTaskFile : Change {
    constructor(path: String): super(path, UndeterminedContents.EMPTY)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      task.removeTaskFile(path)
    }

    override fun apply(state: MutableMap<String, FileContents>) {
      state[path] = contents
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(Change::class.java)

    @Throws(IOException::class)
    fun writeChange(change: Change, out: DataOutput) {
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
    fun readChange(input: DataInput): Change {
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
