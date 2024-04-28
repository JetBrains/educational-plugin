package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CSharpCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun taskTemplateName(course: Course): String = CSharpConfigurator.TASK_CS

  override fun testTemplateName(course: Course): String = CSharpConfigurator.TEST_CS
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getDefaultSettings(): Result<CSharpProjectSettings, String> = Ok(CSharpProjectSettings())
}
