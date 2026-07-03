package com.jetbrains.edu.python

import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import org.junit.Test

class PyFindTaskFileTest : FindTaskFileTestBase<LanguageEnvironment>() {

  override val defaultSettings: LanguageEnvironment = LanguageEnvironment.NoOp

  override fun setUp() {
    super.setUp()
    disablePythonProviders()
  }

  @Test
  fun `test get task dir`() = doTestGetTaskDir(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py",
          taskDirPath = "./Introduction/Our first program")

  @Test
  fun `test get task for file`() = doTestGetTaskForFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py"
  ) { it.lessons[0].taskList[0] }

  @Test
  fun `test get task file`() = doTestGetTaskFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py"
  ) { it.lessons[0].taskList[0].taskFiles["hello_world.py"]!! }
}
