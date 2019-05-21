package com.jetbrains.edu.scala.sbt

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class ScalaSbtCourseProjectGenerator(builder: ScalaSbtCourseBuilder, course: Course) :
  CourseProjectGenerator<JdkProjectSettings>(builder, course)