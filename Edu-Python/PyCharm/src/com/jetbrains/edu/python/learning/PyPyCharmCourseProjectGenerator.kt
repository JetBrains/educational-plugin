package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGeneratorBase
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyPyCharmCourseProjectGenerator(
  builder: EduCourseBuilder<PyNewProjectSettings>,
  course: Course
) : PyCourseProjectGeneratorBase(builder, course) {

  override fun getAllSdks(): List<Sdk> = PyConfigurableInterpreterList.getInstance(null).allPythonSdks

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? = sdk
}
