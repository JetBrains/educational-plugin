package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.csharp.CSharpLanguageSettings
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CSharpHyperskillCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpHyperskillProjectGenerator(this, course)
}