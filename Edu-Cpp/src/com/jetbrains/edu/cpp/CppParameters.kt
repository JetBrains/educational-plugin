package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.course.StepikCourse

private const val UNDEFINED = "<undefined>"

data class CppParameters(val mainCMakeList: String, val taskCMakeList: String, val initCMakeList: String = UNDEFINED)

fun getCppParameters(course: Course): CppParameters = when (course) {
  is StepikCourse -> CppParameters("StepikMainCMakeList.txt", "StepikTaskCMakeList.txt")
  else -> CppParameters("EduMainCMakeList.txt", "EduTaskCMakeList.txt")
}