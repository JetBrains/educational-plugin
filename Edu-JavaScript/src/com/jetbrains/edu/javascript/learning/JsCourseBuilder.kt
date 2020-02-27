package com.jetbrains.edu.javascript.learning

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class JsCourseBuilder : EduCourseBuilder<JsNewProjectSettings> {
  override val taskTemplateName: String = JsConfigurator.TASK_JS
  override val testTemplateName: String = JsConfigurator.TEST_JS

  override fun getLanguageSettings(): LanguageSettings<JsNewProjectSettings> = JsLanguageSettings()
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JsNewProjectSettings> =
    JsCourseProjectGenerator(this, course)
}
