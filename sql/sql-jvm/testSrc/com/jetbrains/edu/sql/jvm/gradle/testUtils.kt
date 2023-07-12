package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse

fun sqlCourse(
  courseMode: CourseMode = CourseMode.STUDENT,
  testLanguage: SqlTestLanguage = SqlTestLanguage.KOTLIN,
  buildCourse: CourseBuilder.() -> Unit
): EduCourse {
  return course(
    language = SqlLanguage.INSTANCE,
    courseMode = courseMode,
    buildCourse = buildCourse
  ).apply {
    sqlTestLanguage = testLanguage
  } as EduCourse
}
