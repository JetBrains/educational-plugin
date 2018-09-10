package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils

object LocalCourseFileChooser : FileChooserDescriptor(true, false, false,
                                                      true, false, false) {
  override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean {
    return file.isDirectory || EduUtils.isZip(file.name)
  }

  override fun isFileSelectable(file: VirtualFile): Boolean {
    return EduUtils.isZip(file.name)
  }
}