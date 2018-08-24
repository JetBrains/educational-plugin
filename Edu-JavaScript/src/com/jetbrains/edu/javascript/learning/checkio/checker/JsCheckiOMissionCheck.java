package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.checkio.checker.CheckiOMissionCheck;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOMissionCheck extends CheckiOMissionCheck {
  protected JsCheckiOMissionCheck(@NotNull Project project, @NotNull Task task) {
    super(project, task, JsCheckiOOAuthConnector.getInstance());
  }

  @Override
  protected String getInterpreter() {
    return JsCheckiONames.JS_CHECKIO_INTERPRETER;
  }

  @Override
  protected String getTestFormTargetUrl() {
    return JsCheckiONames.JS_CHECKIO_TEST_FORM_TARGET_URL;
  }
}
