@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.sourceDir: String? get() = course?.sourceDir
val Task.testDir: String? get() = course?.testDir

val Task.testTextMap: Map<String, String> get() {
  val course = course ?: return emptyMap()
  val testDir = course.testDir ?: return emptyMap()

  val additionalMaterials = course.additionalMaterialsTask
  val testMap = if (testsText.isEmpty() && additionalMaterials != null) {
    val taskDir = "${EduNames.LESSON}${lesson.index}/${EduNames.TASK}$index/"
    additionalMaterials.testsText
      .filterKeys { key -> key.startsWith(taskDir) }
      .mapKeys { (key, _) -> key.removePrefix(taskDir).removePrefix("${EduNames.SRC}/") }
  } else {
    testsText
  }
  return if (testDir.isEmpty()) testMap else testMap.mapKeys { (path, _) -> "$testDir/$path" }
}

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDir(taskDir: VirtualFile): VirtualFile? {
  val testDir = testDir ?: return null
  return taskDir.findFileByRelativePath(testDir)
}
