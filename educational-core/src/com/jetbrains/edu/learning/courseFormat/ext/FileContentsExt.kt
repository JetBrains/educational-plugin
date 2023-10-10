package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.isToEncodeContent

/**
 * The intellij platform is able to determine file types for [VirtualFile]s.
 * It is needed in the plugin to tell binary files from textual files.
 * We do not want to create real virtual files just to get their file type, so we use these [FakeVirtualFile]s.
 *
 * A number of methods are not implemented because they are not called (hopefully) during determining the file type.
 */
private class FakeVirtualFile(private val fakePath: String, val directory: Boolean) : StubVirtualFile() {
  override fun getName(): String {
    val i = fakePath.lastIndexOf('/')
    return if (i >= 0) path.substring(i + 1) else path
  }

  override fun getPath(): String = fakePath

  override fun isDirectory(): Boolean = directory

  override fun isValid(): Boolean = true

  /**
   * Sometimes, to determine the type, the platform examines the name of the parent folder.
   * That's why we want to act as if we really get the parent folder
   */
  override fun getParent(): VirtualFile? {
    if (this == ROOT) return null

    val i = fakePath.lastIndexOf('/')
    return if (i <= 0) ROOT else FakeVirtualFile(fakePath.substring(0, i), true)
  }

  override fun contentsToByteArray(): ByteArray = byteArrayOf()

  override fun getLength(): Long = 0

  companion object {
    val ROOT = FakeVirtualFile("/", directory = true)
  }
}

fun FileContents.disambiguateContents(path: String): DeterminedContents = disambiguateContents(FakeVirtualFile(path, directory = false))

fun FileContents.disambiguateContents(file: VirtualFile): DeterminedContents = when (this) {
  is DeterminedContents -> this
  is UndeterminedContents -> if (file.isToEncodeContent) {
    InMemoryBinaryContents(bytes)
  }
  else {
    InMemoryTextualContents(text)
  }
}