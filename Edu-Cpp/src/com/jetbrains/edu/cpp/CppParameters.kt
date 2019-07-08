package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.course.StepikCourse

abstract class CppParameters(val mainCMakeList: String, val taskCMakeList: String)

object EduParameters : CppParameters("EduMainCMakeList.txt", "EduTaskCMakeList.txt")

object StepikParameters : CppParameters("StepikMainCMakeList.txt", "StepikTaskCMakeList.txt")

fun getParametersByCourse(course: Course): CppParameters = when (course) {
  is StepikCourse -> StepikParameters
  else -> EduParameters
}