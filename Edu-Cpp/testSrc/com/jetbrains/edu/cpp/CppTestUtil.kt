package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course

fun getExpectedTaskCMakeText(course: Course, projectSettings: CppProjectSettings, expectedProjectName: String): String =
  getCppTemplates(course).taskCMakeList.getText(expectedProjectName, projectSettings.languageStandard)