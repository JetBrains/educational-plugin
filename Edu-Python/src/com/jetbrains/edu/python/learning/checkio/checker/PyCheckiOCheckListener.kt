package com.jetbrains.edu.python.learning.checkio.checker

import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.checker.CheckiOCheckListener
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.PythonLanguage

class PyCheckiOCheckListener : CheckiOCheckListener(
  CheckiOCourseContentGenerator(
    PythonFileType.INSTANCE,
    PyCheckiOApiConnector
  ),
  PyCheckiOOAuthConnector
) {
  override fun isEnabledForCourse(course: CheckiOCourse): Boolean {
    return PythonLanguage.INSTANCE === course.languageById
  }
}
