package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException

class BinaryContentsFromDisk(val file: VirtualFile) : BinaryContents {
  override val bytes: ByteArray
    get() = runReadAction {
      try {
        file.contentsToByteArray()
      }
      catch (_: FileTooBigException) {
        throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong())
      }
    }
}

class TextualContentsFromDisk(val file: VirtualFile) : TextualContents {
  override val text: String
    get() = runReadAction {
      VfsUtilCore.loadText(file)
    }
}