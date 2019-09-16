package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.course.StepikCourse

/** Provides CMake file template names, where:
 * [mainCMakeList] - configures the course project, e.g. find all tasks `CMakeList.txt` files and adds them.
 * [taskCMakeList] - configures the task project.
 * [testCMakeList] - specializes settings for loading a test framework, for Course Creator project only.
 */
data class CppCMakeTemplateNames(
  val mainCMakeList: String,
  val taskCMakeList: String,
  val testCMakeList: String = "",
  val runTestsCpp: String = ""
)

fun getCppCMakeTemplateNames(course: Course): CppCMakeTemplateNames =
  if (course is StepikCourse)
    CppCMakeTemplateNames("StepikMainCMakeList.txt", "StepikTaskCMakeList.txt")
  else
    CppCMakeTemplateNames("EduMainCMakeList.txt", "EduTaskCMakeList.txt",
                          "EduTestCMakeList.txt.in", "runTests.cpp")