package com.jetbrains.edu.learning.editor

import com.intellij.openapi.extensions.InternalIgnoreDependencyViolation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider
import com.jetbrains.edu.learning.course

@InternalIgnoreDependencyViolation
class EditableFileProvider(private val myProject: Project) : WritingAccessProvider() {

  override fun requestWriting(files: MutableCollection<out VirtualFile>): MutableCollection<VirtualFile> {
    val course = myProject.course ?: return mutableListOf()
    return files.filter { !course.isEditableFile(it.path) }.toMutableList()
  }

  override fun isPotentiallyWritable(file: VirtualFile): Boolean {
    return myProject.course?.isEditableFile(file.path) ?: true
  }
}