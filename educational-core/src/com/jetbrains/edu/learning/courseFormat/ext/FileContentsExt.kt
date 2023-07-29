package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.isToEncodeContent
import java.io.InputStream
import java.io.OutputStream

/**
 * The intellij platform is able to determine file types for [VirtualFile]s.
 * It is needed in the plugin to tell binary files from textual files.
 * We do not want to create real virtual files just to get their file type, so we use these [FakeVirtualFile]s.
 *
 * A number of methods are not implemented because they are not called (hopefully) during determining the file type.
 */
class FakeVirtualFile(private val fakePath: String) : VirtualFile() {
  override fun getName(): String {
    val i = fakePath.lastIndexOf('/')
    return if (i > 0) path.substring(i + 1) else path
  }

  override fun getFileSystem(): VirtualFileSystem = LocalFileSystem.getInstance()

  override fun getPath(): String = fakePath

  override fun isWritable(): Boolean = TODO("This method is not expected to be called")

  override fun isDirectory(): Boolean = false

  override fun isValid(): Boolean = true

  /**
   * Sometimes, to determine the type, the platform examines the name of the parent folder.
   * That's why we want to act as if we really get the parent folder
   */
  override fun getParent(): VirtualFile? {
    if (this == ROOT) return null

    val i = fakePath.lastIndexOf('/')
    return if (i <= 0) ROOT else FakeVirtualFile(fakePath.substring(0, i))
  }

  override fun getChildren(): Array<VirtualFile> = TODO("This method is not expected to be called")

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream = TODO("This method is not expected to be called")

  override fun contentsToByteArray(): ByteArray = byteArrayOf()

  override fun getTimeStamp(): Long = TODO("This method is not expected to be called")

  override fun getLength(): Long = 0

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) = TODO("This method is not expected to be called")

  override fun getInputStream(): InputStream = TODO("This method is not expected to be called")

  companion object {
    val ROOT = FakeVirtualFile("/")
  }
}

fun FileContents.disambiguateContents(path: String): DeterminedContents = when (this) {
  is DeterminedContents -> this
  is UndeterminedContents -> if (FakeVirtualFile(path).isToEncodeContent) {
    InMemoryBinaryContents(bytes)
  }
  else {
    InMemoryTextualContents(text)
  }
}