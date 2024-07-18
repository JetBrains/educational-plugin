package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.lang.javascript.JavaScriptFileType
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.checkio.checker.JsCheckiOTaskCheckerProvider
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames.JS_CHECKIO_URL
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import javax.swing.Icon

class JsCheckiOConfigurator : JsConfigurator(), CheckiOConnectorProvider {
  override val taskCheckerProvider: TaskCheckerProvider
    get() = JsCheckiOTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Platform.JSCheckiO

  override val isCourseCreatorEnabled: Boolean = false

  override fun beforeCourseStarted(course: Course) {
    val contentGenerator = CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector)
    getCourseFromServerUnderProgress(contentGenerator, course as CheckiOCourse, JsCheckiOSettings.getInstance().account, JS_CHECKIO_URL)
  }

  override val oAuthConnector: CheckiOOAuthConnector
    get() = JsCheckiOOAuthConnector
}