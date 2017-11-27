package com.jetbrains.edu.java

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

class JFindTaskTest : FindTaskFileTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test get task dir`() = doTestGetTaskDir(
          pathToCourseJson = "testData/newCourse/java_course.json",
          filePath = "./lesson1/task1/src/Task.java",
          taskDirPath = "./lesson1/task1")

  fun `test get task for file`() = doTestGetTaskForFile(
          pathToCourseJson = "testData/newCourse/java_course.json",
          filePath = "./lesson1/task1/src/Task.java"
  ) { it.lessons[0].taskList[0] }

  fun `test get task file`() = doTestGetTaskFile(
          pathToCourseJson = "testData/newCourse/java_course.json",
          filePath = "./lesson1/task1/src/Task.java"
  ) { it.lessons[0].taskList[0].taskFiles["Task.java"]!! }
}
