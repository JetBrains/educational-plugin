package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

fun fileTree(block: FileTreeBuilder.() -> Unit): FileTree = FileTree(FileTreeBuilderImpl().apply(block).intoDirectory())

interface FileTreeBuilder {
  fun dir(name: String, block: FileTreeBuilder.() -> Unit)
  fun file(name: String, code: String = "")
}

class FileTree(private val rootDirectory: Entry.Directory) {

  fun assertEquals(baseDir: VirtualFile) {

    fun go(expected: Entry.Directory, actual: VirtualFile) {
      val actualChildren = actual.children.associateBy { it.name }
      check(expected.children.keys == actualChildren.keys) {
        "Mismatch in directory ${actual.path}\n" +
                "Expected: ${expected.children.keys}\n" +
                "Actual  : ${actualChildren.keys}"
      }

      for ((name, entry) in expected.children) {
        val child = actualChildren[name]!!
        when (entry) {
          is Entry.File -> check(!child.isDirectory)
          is Entry.Directory -> go(entry, child)
        }
      }
    }

    fullyRefreshDirectory(baseDir)
    go(rootDirectory, baseDir)
  }
}

fun fullyRefreshDirectory(directory: VirtualFile) {
  VfsUtil.markDirtyAndRefresh(false, true, true, directory)
}

private class FileTreeBuilderImpl(val directory: MutableMap<String, Entry> = mutableMapOf()) : FileTreeBuilder {
  override fun dir(name: String, block: FileTreeBuilder.() -> Unit) {
    check('/' !in name) { "Bad directory name `$name`" }
    directory[name] = FileTreeBuilderImpl().apply(block).intoDirectory()
  }

  override fun file(name: String, code: String) {
    check('/' !in name && '.' in name) { "Bad file name `$name`" }
    directory[name] = Entry.File(code.trimIndent())
  }

  fun intoDirectory() = Entry.Directory(directory)
}

sealed class Entry {
  class File(val text: String) : Entry()
  class Directory(val children: MutableMap<String, Entry>) : Entry()
}
