package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.JsEnvironmentChecker
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames.*
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import javax.swing.Icon

class JsCheckiOConfigurator : JsConfigurator(), CheckiOConnectorProvider {
  override val taskCheckerProvider: TaskCheckerProvider = object : TaskCheckerProvider {
    override val envChecker: EnvironmentChecker
      get() = JsEnvironmentChecker()

    override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
      CheckiOTaskChecker(task, envChecker, project, JsCheckiOOAuthConnector,
                         JS_CHECKIO_INTERPRETER, JS_CHECKIO_TEST_FORM_TARGET_URL)
  }

  override fun getOAuthConnector(): CheckiOOAuthConnector = JsCheckiOOAuthConnector

  override val logo: Icon
    get() = EducationalCoreIcons.JSCheckiO

  override val isCourseCreatorEnabled: Boolean
    get() = false

  override fun beforeCourseStarted(course: Course) {
    val contentGenerator = CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance())
    getCourseFromServerUnderProgress(contentGenerator, course as CheckiOCourse, JsCheckiOSettings.getInstance().account,
                                     JS_CHECKIO_API_HOST)
  }
}