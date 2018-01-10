@file:JvmName("TaskExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Task.course: Course? get() = lesson?.course

val Task.taskFilesDir: String? get() = course?.taskFilesDir
val Task.testFilesDir: String? get() = course?.testFilesDir

val Task.testTextMap: Map<String, String> get() {
  val course = course ?: return emptyMap()
  val testFilesDir = course.testFilesDir ?: return emptyMap()

  val additionalMaterials = course.additionalMaterialsTask
  val testMap = if (testsText.isEmpty() && additionalMaterials != null) {
    val taskDir = "${EduNames.LESSON}${lesson.index}/${EduNames.TASK}$index/"
    additionalMaterials.testsText
      .filterKeys { key -> key.startsWith(taskDir) }
      .mapKeys { (key, _) -> key.removePrefix(taskDir).removePrefix("${EduNames.SRC}/") }
  } else {
    testsText
  }
  return if (testFilesDir.isEmpty()) testMap else testMap.mapKeys { (path, _) -> "$testFilesDir/$path" }
}

fun Task.findTasksDir(taskDir: VirtualFile): VirtualFile? {
  val testFilesDir = taskFilesDir ?: return null
  return taskDir.findFileByRelativePath(testFilesDir)
}

fun Task.findTestsDir(taskDir: VirtualFile): VirtualFile? {
  val testFilesDir = testFilesDir ?: return null
  return taskDir.findFileByRelativePath(testFilesDir)
}
