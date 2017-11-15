package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class EduTaskChecker extends TaskChecker<EduTask> {
  public EduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }
}
