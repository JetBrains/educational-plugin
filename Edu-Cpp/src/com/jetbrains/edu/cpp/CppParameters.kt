package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.course.StepikCourse

data class CppParameters(val mainCMakeList: String, val taskCMakeList: String)

fun getCppParameters(course: Course): CppParameters = when (course) {
  is StepikCourse -> CppParameters("StepikMainCMakeList.txt", "StepikTaskCMakeList.txt")
  else -> CppParameters("EduMainCMakeList.txt", "EduTaskCMakeList.txt")
}