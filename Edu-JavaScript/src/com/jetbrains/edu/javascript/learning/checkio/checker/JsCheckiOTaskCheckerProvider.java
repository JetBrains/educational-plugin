package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOTaskCheckerProvider implements TaskCheckerProvider {
  @NotNull
  @Override
  public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    return new JsCheckiOTaskChecker(task, project);
  }
}
