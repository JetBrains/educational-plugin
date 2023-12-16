package com.jetbrains.edu.learning

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.CourseArchiveIndicator
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents

class BinaryContentsFromDisk(val file: VirtualFile, private val indicator: CourseArchiveIndicator?) : BinaryContents {
  override val bytes: ByteArray
    get() {
      indicator?.readFile(file)

      return runReadAction {
        file.contentsToByteArray()
      }
    }
}

class TextualContentsFromDisk(val file: VirtualFile, private val indicator: CourseArchiveIndicator?) : TextualContents {
  override val text: String
    get() {
      indicator?.readFile(file)

      return runReadAction {
        VfsUtilCore.loadText(file)
      }
    }
}