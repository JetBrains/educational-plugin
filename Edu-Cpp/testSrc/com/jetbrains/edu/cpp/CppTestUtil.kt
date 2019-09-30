package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course

fun getExpectedTaskCMakeText(course: Course, projectSettings: CppProjectSettings, expectedProjectName: String): String =
  getCppTemplates(course).taskCMakeList.getText { key ->
    when (key) {
      CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KEY -> cMakeMinimumRequired
      CppTemplates.PROJECT_NAME_KEY -> expectedProjectName
      CppTemplates.CPP_STANDARD_LINE_KEY -> projectSettings.languageStandard
      else -> ""
    }
  }