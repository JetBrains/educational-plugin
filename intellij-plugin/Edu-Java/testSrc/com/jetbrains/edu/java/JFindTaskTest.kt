package com.jetbrains.edu.java

import com.jetbrains.edu.jvm.JvmFindTaskFileTestBase
import org.junit.Test

class JFindTaskTest : JvmFindTaskFileTestBase() {

  @Test
  fun `test get task dir`() = doTestGetTaskDir(
    pathToCourseJson = "testData/newCourse/java_course.json",
    filePath = "./lesson1/task1/src/Task.java",
    taskDirPath = "./lesson1/task1")

  @Test
  fun `test get task for file`() = doTestGetTaskForFile(
    pathToCourseJson = "testData/newCourse/java_course.json",
    filePath = "./lesson1/task1/src/Task.java"
  ) { it.lessons[0].taskList[0] }

  @Test
  fun `test get task file`() = doTestGetTaskFile(
    pathToCourseJson = "testData/newCourse/java_course.json",
    filePath = "./lesson1/task1/src/Task.java"
  ) { it.lessons[0].taskList[0].taskFiles["src/Task.java"]!! }
}
