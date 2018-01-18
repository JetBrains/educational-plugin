@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.sourceDir: String? get() = course?.sourceDir
val Task.testDir: String? get() = course?.testDir

val Task.testTextMap: Map<String, String> get() {
  val course = course ?: return emptyMap()
  val testDir = course.testDir ?: return emptyMap()
  return if (testDir.isEmpty()) testsText else testsText.mapKeys { (path, _) -> "$testDir/$path" }
}

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDir(taskDir: VirtualFile): VirtualFile? {
  val testDir = testDir ?: return null
  return taskDir.findFileByRelativePath(testDir)
}
