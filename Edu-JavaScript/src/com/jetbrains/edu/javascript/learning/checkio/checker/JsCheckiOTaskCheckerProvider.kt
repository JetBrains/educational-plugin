package com.jetbrains.edu.javascript.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.JsEnvironmentChecker
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class JsCheckiOTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = JsEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    CheckiOTaskChecker(task, envChecker, project, JsCheckiOOAuthConnector,
                       JsCheckiONames.JS_CHECKIO_INTERPRETER, JsCheckiONames.JS_CHECKIO_TEST_FORM_TARGET_URL)
}