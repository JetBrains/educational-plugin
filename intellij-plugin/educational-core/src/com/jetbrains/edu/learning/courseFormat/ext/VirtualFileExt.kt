package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir

fun VirtualFile.pathInCourse(project: Project): String? = VfsUtilCore.getRelativePath(this, project.courseDir)
fun VirtualFile.pathInCourse(holder: CourseInfoHolder<*>): String? = VfsUtilCore.getRelativePath(this, holder.courseDir)