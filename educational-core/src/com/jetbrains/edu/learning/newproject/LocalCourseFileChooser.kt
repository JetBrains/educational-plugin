package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtilsKt.isZip

object LocalCourseFileChooser : FileChooserDescriptor(true, false, false,
                                                      true, false, false) {
  override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean {
    return file.isDirectory || isZip(file.name)
  }

  override fun isFileSelectable(file: VirtualFile?): Boolean {
    return file != null && isZip(file.name)
  }

  override fun isForcedToUseIdeaFileChooser(): Boolean = true
}