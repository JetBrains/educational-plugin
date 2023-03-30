package com.jetbrains.edu.javascript.learning.checkio.checker

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.checker.CheckiOCheckListener
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.ext.languageById

class JsCheckiOCheckListener : CheckiOCheckListener(
  CheckiOCourseContentGenerator(
    JavaScriptFileType.INSTANCE,
    JsCheckiOApiConnector
  ),
  JsCheckiOOAuthConnector
) {
  override fun isEnabledForCourse(course: CheckiOCourse): Boolean {
    return course.languageById === JavascriptLanguage.INSTANCE
  }
}
