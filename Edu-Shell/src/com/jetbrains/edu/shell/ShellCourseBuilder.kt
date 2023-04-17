package com.jetbrains.edu.shell

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class ShellCourseBuilder : EduCourseBuilder<EmptyProjectSettings> {
  override val taskTemplateName: String = ShellConfigurator.TASK_SH

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<EmptyProjectSettings> =
    ShellCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<EmptyProjectSettings> = ShellLanguageSettings()
}