package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater;
import com.jetbrains.edu.learning.checkio.checker.CheckiOMissionCheck;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOTaskChecker extends CheckiOTaskChecker {

  protected JsCheckiOTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project, JsCheckiOOAuthConnector.getInstance());
  }

  @NotNull
  @Override
  protected CheckiOMissionCheck getMissionCheck() {
    return new JsCheckiOMissionCheck(project, task);
  }

  @NotNull
  @Override
  protected CheckiOCourseUpdater getCourseUpdater() {
    final CheckiOCourseContentGenerator contentGenerator =
      new CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance());

    return new CheckiOCourseUpdater(
      (CheckiOCourse) task.getCourse(),
      project,
      contentGenerator
    );
  }
}
