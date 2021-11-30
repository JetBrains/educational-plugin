package com.jetbrains.edu.learning.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider
import com.jetbrains.edu.learning.course

class EditableFileProvider(private val project: Project) : WritingAccessProvider() {

  override fun requestWriting(files: Collection<VirtualFile>): Collection<VirtualFile> {
    val course = project.course
    // educators can modify all files
    if (course == null || !course.isStudy) {
      return listOf()
    }
    return files.filter { !course.isEditableFile(it.path) }
  }

  override fun isPotentiallyWritable(file: VirtualFile): Boolean {
    val course = project.course
    // educators can modify all files
    if (course == null || !course.isStudy) {
      return true
    }
    return course.isEditableFile(file.path)
  }
}