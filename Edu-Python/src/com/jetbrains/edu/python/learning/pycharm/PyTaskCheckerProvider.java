package com.jetbrains.edu.python.learning.pycharm;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.OutputTaskChecker;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TheoryTaskChecker;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.python.learning.checker.PyTaskChecker;
import org.jetbrains.annotations.NotNull;

public class PyTaskCheckerProvider implements TaskCheckerProvider {
    @NotNull
    @Override
    public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
        return new PyTaskChecker(task, project);
    }

    @NotNull
    @Override
    public TaskChecker<OutputTask> getOutputTaskChecker(@NotNull OutputTask task, @NotNull Project project) {
        return new OutputTaskChecker(task, project);
    }

    @NotNull
    @Override
    public TaskChecker<TheoryTask> getTheoryTaskChecker(@NotNull TheoryTask task, @NotNull Project project) {
        return new TheoryTaskChecker(task, project);
    }
}
