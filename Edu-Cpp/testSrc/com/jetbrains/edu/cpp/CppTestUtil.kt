package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

fun getExpectedTaskCMakeText(course: Course, projectSettings: CppProjectSettings, expectedProjectName: String): String {
  val cMakeName = getCppCMakeTemplateNames(course).taskCMakeList
  val cMakeVariables = getCMakeTemplateVariables(expectedProjectName, projectSettings.languageStandard)
  return GeneratorUtils.getInternalTemplateText(cMakeName, cMakeVariables)
}