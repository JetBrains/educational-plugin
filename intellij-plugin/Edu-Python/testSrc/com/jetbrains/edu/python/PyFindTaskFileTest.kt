package com.jetbrains.edu.python

import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

class PyFindTaskFileTest : FindTaskFileTestBase<PyProjectSettings>() {

  override val defaultSettings: PyProjectSettings = PyProjectSettings()

  fun `test get task dir`() = doTestGetTaskDir(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py",
          taskDirPath = "./Introduction/Our first program")

  fun `test get task for file`() = doTestGetTaskForFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py"
  ) { it.lessons[0].taskList[0] }

  fun `test get task file`() = doTestGetTaskFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./Introduction/Our first program/hello_world.py"
  ) { it.lessons[0].taskList[0].taskFiles["hello_world.py"]!! }
}
