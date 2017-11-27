package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

class KtFindTaskFileTest : FindTaskFileTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test get task dir`() = doTestGetTaskDir(
          pathToCourseJson = "testData/newCourse/kotlin_course.json",
          filePath = "./lesson1/task1/src/Task.kt",
          taskDirPath = "./lesson1/task1")

  fun `test get task for file`() = doTestGetTaskForFile(
          pathToCourseJson = "testData/newCourse/kotlin_course.json",
          filePath = "./lesson1/task1/src/Task.kt"
  ) { it.lessons[0].taskList[0] }

  fun `test get task file`() = doTestGetTaskFile(
          pathToCourseJson = "testData/newCourse/kotlin_course.json",
          filePath = "./lesson1/task1/src/Task.kt"
  ) { it.lessons[0].taskList[0].taskFiles["Task.kt"]!! }
}
