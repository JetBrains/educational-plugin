package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskChecker;
import com.jetbrains.edu.learning.codeforces.checker.CodeforcesTaskWithFileIOTaskChecker;
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask;
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import com.jetbrains.edu.learning.handlers.CodeExecutor;
import com.jetbrains.edu.learning.handlers.DefaultCodeExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TaskCheckerProvider {
    @NotNull
    TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project);

    @NotNull
    default OutputTaskChecker getOutputTaskChecker(@NotNull OutputTask task, @NotNull Project project, @NotNull CodeExecutor codeExecutor) {
        return new OutputTaskChecker(task, project, codeExecutor);
    }

    @NotNull
    default TheoryTaskChecker getTheoryTaskChecker(@NotNull TheoryTask task, @NotNull Project project) {
        return new TheoryTaskChecker(task, project);
    }

    @Nullable
    default TaskChecker<ChoiceTask> getChoiceTaskChecker(@NotNull ChoiceTask task, @NotNull Project project) {
        return task.getCanCheckLocally() ? new ChoiceTaskChecker(task, project) : null;
    }

    @Nullable
    default TaskChecker<CodeTask> getCodeTaskChecker(@NotNull CodeTask task, @NotNull Project project) {
        return null;
    }

    @NotNull
    default TaskChecker<IdeTask> getIdeTaskChecker(@NotNull IdeTask task, @NotNull Project project) {
        return new IdeTaskChecker(task, project);
    }

    @NotNull
    default CodeExecutor getCodeExecutor() {
      return new DefaultCodeExecutor();
    }

    @Nullable
    default TaskChecker getTaskChecker(@NotNull Task task, @NotNull Project project) {
        if (task instanceof EduTask) {
            return getEduTaskChecker((EduTask) task, project);
        }
        else if (task instanceof OutputTask) {
            return getOutputTaskChecker((OutputTask) task, project, getCodeExecutor());
        }
        else if (task instanceof TheoryTask) {
            return getTheoryTaskChecker((TheoryTask) task, project);
        }
        else if (task instanceof CodeTask) {
            return getCodeTaskChecker((CodeTask) task, project);
        }
        else if (task instanceof ChoiceTask) {
            return getChoiceTaskChecker((ChoiceTask) task, project);
        }
        else if (task instanceof IdeTask) {
            return getIdeTaskChecker((IdeTask) task, project);
        }
        else if (task instanceof CodeforcesTask) {
            if (task instanceof CodeforcesTaskWithFileIO) {
                return new CodeforcesTaskWithFileIOTaskChecker((CodeforcesTaskWithFileIO) task, project);
            }
            return new CodeforcesTaskChecker((CodeforcesTask) task, project, getCodeExecutor());
        }
        else {
            throw new IllegalStateException("Unknown task type: " + task.getItemType());
        }
    }
}
