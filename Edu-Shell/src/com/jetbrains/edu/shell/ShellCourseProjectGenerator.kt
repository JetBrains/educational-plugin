package com.jetbrains.edu.shell

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class ShellCourseProjectGenerator(
  builder: EduCourseBuilder<EmptyProjectSettings>,
  course: Course
) : CourseProjectGenerator<EmptyProjectSettings>(builder, course)