package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames

class PyCheckiOWidgetProvider : LoginWidgetProvider() {

  override fun createLoginWidget(project: Project) = PyCheckiOWidget(project)

  override fun isWidgetAvailable(courseType: String,
                                 languageId: String?) = courseType == CheckiONames.CHECKIO && languageId == EduNames.PYTHON

  override fun isWidgetAvailable(course: Course) = course is CheckiOCourse && course.name == PyCheckiONames.PY_CHECKIO
}