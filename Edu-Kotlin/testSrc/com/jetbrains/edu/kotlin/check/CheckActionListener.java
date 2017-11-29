package com.jetbrains.edu.kotlin.check;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class CheckActionListener implements CheckListener {
    // This field can be modified if some special checks are needed (return true if should run standard checks)
    @SuppressWarnings("StaticNonFinalField")
    public static Function1<Task, Boolean> afterCheck = null;
    @Override
    public void afterCheck(@NotNull Project project, @NotNull Task task) {
        boolean shouldRunStandardCheck = true;
        if (afterCheck != null) {
            shouldRunStandardCheck = afterCheck.invoke(task);
        }

        if (shouldRunStandardCheck) {
            String taskName = task.getLesson().getName() + "/" + task.getName();
            Assert.assertFalse("Check Task Action failed for " + taskName, task.getStatus() == CheckStatus.Failed);
            Assert.assertFalse("Check Task Action skipped for " + taskName, task.getStatus() == CheckStatus.Unchecked);
            System.out.println("Check for " + taskName + " passed");
        }
    }
}
