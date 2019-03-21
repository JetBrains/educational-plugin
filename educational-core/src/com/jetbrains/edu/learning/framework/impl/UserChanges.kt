package com.jetbrains.edu.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class UserChanges(val changes: List<Change>) {

  operator fun plus(otherChanges: List<Change>): UserChanges = UserChanges(changes + otherChanges)

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
  fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
  }

  companion object {

    private val EMPTY = UserChanges(emptyList())

    @JvmStatic
    fun empty(): UserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      return UserChanges(changes)
    }
  }
}

sealed class Change {

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

  class AddFile : Change {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      try {
        GeneratorUtils.createChildFile(taskDir, path, text)
      } catch (e: IOException) {
        LOG.error("Failed to create file `${taskDir.path}/$path`", e)
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveFile : Change {

    constructor(path: String): super(path, "")
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      runUndoTransparentWriteAction {
        try {
          taskDir.findFileByRelativePath(path)?.delete(RemoveFile::class.java)
        } catch (e: IOException) {
          LOG.error("Failed to delete file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state -= path
    }
  }

  class ChangeFile : Change {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val file = taskDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("Can't find file `$path` in `$taskDir`")
        return
      }

      val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
      if (document == null) {
        LOG.warn("Can't get document for `$file`")
      } else {
        runUndoTransparentWriteAction { document.setText(text) }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class AddUserCreatedTaskFile : Change {

    constructor(path: String, text: String): super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val taskFile = TaskFile(path, text)
      taskFile.isUserCreated = true
      task.addTaskFile(taskFile)
    }


    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveTaskFile : Change {

    constructor(path: String): super(path, "")
    @Throws(IOException::class)
    constructor(input: DataInput): super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      task.taskFiles.remove(path)
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(Change::class.java)

    @JvmStatic
    @Throws(IOException::class)
    fun writeChange(change: Change, out: DataOutput) {
      val ordinal = when (change) {
        is AddFile -> 0
        is RemoveFile -> 1
        is ChangeFile -> 2
        is AddUserCreatedTaskFile -> 3
        is RemoveTaskFile -> 4
      }
      out.writeInt(ordinal)
      change.write(out)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readChange(input: DataInput): Change {
      val ordinal = input.readInt()
      return when (ordinal) {
        0 -> AddFile(input)
        1 -> RemoveFile(input)
        2 -> ChangeFile(input)
        3 -> AddUserCreatedTaskFile(input)
        4 -> RemoveTaskFile(input)
        else -> error("Unexpected change type: $ordinal")
      }
    }
  }
}
