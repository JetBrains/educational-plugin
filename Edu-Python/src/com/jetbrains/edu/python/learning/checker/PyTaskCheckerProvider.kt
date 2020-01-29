package com.jetbrains.edu.python.learning.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class PyTaskCheckerProvider implements TaskCheckerProvider {
    @NotNull
    @Override
    public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
        return new PyTaskChecker(task, project);
    }
}
