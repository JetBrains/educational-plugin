package com.jetbrains.edu.python

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.newProject.PythonProjectGenerator

class PyFindTaskFileTest : FindTaskFileTestBase<PyNewProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<PyNewProjectSettings> = PyCourseBuilder()
  override val defaultSettings: PyNewProjectSettings = PythonProjectGenerator.NO_SETTINGS

  fun `test get task dir`() = doTestGetTaskDir(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./lesson1/task1/hello_world.py",
          taskDirPath = "./lesson1/task1")

  fun `test get task for file`() = doTestGetTaskForFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./lesson1/task1/hello_world.py"
  ) { it.lessons[0].taskList[0] }

  fun `test get task file`() = doTestGetTaskFile(
          pathToCourseJson = "testData/newCourse/python_course.json",
          filePath = "./lesson1/task1/hello_world.py"
  ) { it.lessons[0].taskList[0].taskFiles["hello_world.py"]!! }
}
