package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.JvmFindTaskFileTestBase
import org.junit.Test

class KtFindTaskFileTest : JvmFindTaskFileTestBase() {

  @Test
  fun `test get task dir`() = doTestGetTaskDir(
    pathToCourseJson = "testData/newCourse/kotlin_course.json",
    filePath = "./Introduction/Hello, world/src/Task.kt",
    taskDirPath = "./Introduction/Hello, world")

  @Test
  fun `test get task for file`() = doTestGetTaskForFile(
    pathToCourseJson = "testData/newCourse/kotlin_course.json",
    filePath = "./Introduction/Hello, world/src/Task.kt"
  ) { it.lessons[0].taskList[0] }

  @Test
  fun `test get task file`() = doTestGetTaskFile(
    pathToCourseJson = "testData/newCourse/kotlin_course.json",
    filePath = "./Introduction/Hello, world/src/Task.kt"
  ) { it.lessons[0].taskList[0].taskFiles["src/Task.kt"]!! }
}
