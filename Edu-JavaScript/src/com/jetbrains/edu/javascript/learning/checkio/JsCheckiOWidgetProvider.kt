package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course

class JsCheckiOWidgetProvider : LoginWidgetProvider() {

  override fun createLoginWidget(project: Project) = JsCheckiOWidget(project)

  override fun isWidgetAvailable(courseType: String,
                                 languageId: String?) = courseType == CheckiONames.CHECKIO && languageId == EduNames.JAVASCRIPT

  override fun isWidgetAvailable(course: Course) = course is CheckiOCourse && course.name == JsCheckiONames.JS_CHECKIO
}