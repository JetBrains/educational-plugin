package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CourseProjectGenerator<CSharpProjectSettings>(builder, course)