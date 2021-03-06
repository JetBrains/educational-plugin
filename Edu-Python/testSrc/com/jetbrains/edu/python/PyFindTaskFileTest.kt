package com.jetbrains.edu.python

import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.newProject.PythonProjectGenerator

class PyFindTaskFileTest : FindTaskFileTestBase<PyNewProjectSettings>() {

  override val defaultSettings: PyNewProjectSettings = PythonProjectGenerator.NO_SETTINGS

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
